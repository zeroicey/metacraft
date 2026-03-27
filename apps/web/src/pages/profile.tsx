import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router";
import { useCurrentUser, useUpdateUser } from "@/hooks/useUser";
import { useAuthStore } from "@/stores/auth-store";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { ArrowLeftIcon, Loader2Icon, CameraIcon } from "lucide-react";
import { toast } from "sonner";

// 获取头像 URL
const getAvatarUrl = (avatarBase64?: string, name?: string) => {
    if (!avatarBase64 || avatarBase64 === "") {
        const seed = encodeURIComponent(name || "user");
        return `https://api.dicebear.com/7.x/pixel-art/svg?seed=${seed}`;
    }
    if (avatarBase64.startsWith("data:")) {
        return avatarBase64;
    }
    return `data:image/png;base64,${avatarBase64}`;
};

export default function ProfilePage() {
    const navigate = useNavigate();
    const { data: user, isLoading, refetch } = useCurrentUser();
    const updateUser = useUpdateUser();
    const logout = useAuthStore((state) => state.logout);

    const [name, setName] = useState("");
    const [bio, setBio] = useState("");
    const [avatarPreview, setAvatarPreview] = useState("");
    const [avatarBase64, setAvatarBase64] = useState("");
    const fileInputRef = useRef<HTMLInputElement>(null);
    const [isInitialized, setIsInitialized] = useState(false);

    // 当用户数据加载完成后初始化表单
    useEffect(() => {
        if (user && !isInitialized) {
            setName(user.name || "");
            setBio(user.bio || "");
            setAvatarPreview(getAvatarUrl(user.avatarBase64, user.name));
            setIsInitialized(true);
        }
    }, [user, isInitialized]);

    const handleAvatarClick = () => {
        fileInputRef.current?.click();
    };

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;

        // 检查文件类型
        if (!file.type.startsWith("image/")) {
            toast.error("请选择图片文件");
            return;
        }

        // 检查文件大小 (最大 2MB)
        if (file.size > 2 * 1024 * 1024) {
            toast.error("图片大小不能超过 2MB");
            return;
        }

        // 转换为 base64
        const reader = new FileReader();
        reader.onload = (event) => {
            const result = event.target?.result as string;
            setAvatarPreview(result);
            setAvatarBase64(result);
        };
        reader.readAsDataURL(file);
    };

    const handleSave = async () => {
        try {
            await updateUser.mutateAsync({
                name,
                bio,
                avatarBase64: avatarBase64 || undefined,
            });
            toast.success("保存成功");
            setAvatarBase64(""); // Clear the base64 after save
            refetch();
        } catch (error) {
            // Error handled by hook
        }
    };

    const handleLogout = () => {
        logout();
        navigate("/login");
    };

    if (isLoading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <Loader2Icon className="h-8 w-8 animate-spin text-gray-400" />
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gradient-to-b from-[#F0F4F8] to-white">
            {/* 头部 */}
            <div className="bg-white/80 backdrop-blur-sm border-b border-[#E5E7EB] px-4 py-3 flex items-center gap-3 sticky top-0 z-10 shadow-sm">
                <Button variant="ghost" size="icon" onClick={() => navigate(-1)}>
                    <ArrowLeftIcon className="h-5 w-5" />
                </Button>
                <h1 className="text-lg font-semibold">个人中心</h1>
            </div>

            <div className="max-w-md mx-auto p-4 space-y-4">
                <div className="bg-white rounded-2xl shadow-lg p-4">
                {/* 头像区域 */}
                <div className="flex flex-col items-center py-6">
                    <div className="relative">
                        <Avatar className="h-24 w-24 cursor-pointer shadow-lg" onClick={handleAvatarClick}>
                            <AvatarImage src={avatarPreview} alt={user?.name} />
                            <AvatarFallback className="text-2xl">
                                {user?.name?.charAt(0)?.toUpperCase() || "U"}
                            </AvatarFallback>
                        </Avatar>
                        <div className="absolute bottom-0 right-0 bg-[#007AFF] text-white p-1.5 rounded-full shadow-md">
                            <CameraIcon className="h-4 w-4" />
                        </div>
                    </div>
                    <input
                        ref={fileInputRef}
                        type="file"
                        accept="image/*"
                        className="hidden"
                        onChange={handleFileChange}
                    />
                    <p className="mt-2 text-sm text-gray-500">点击上传头像</p>
                </div>

                {/* 用户信息 */}
                <Card className="rounded-2xl shadow-md border-[#E5E7EB]">
                    <CardHeader>
                        <CardTitle className="text-base">账户信息</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div>
                            <p className="text-sm text-gray-500 mb-1">用户名</p>
                            <Input
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                placeholder="请输入用户名"
                            />
                        </div>
                        <div>
                            <p className="text-sm text-gray-500 mb-1">邮箱</p>
                            <Input value={user?.email || ""} disabled className="bg-gray-50" />
                        </div>
                        <div>
                            <p className="text-sm text-gray-500 mb-1">简介</p>
                            <Textarea
                                value={bio}
                                onChange={(e) => setBio(e.target.value)}
                                placeholder="请输入简介"
                                rows={3}
                            />
                        </div>

                        <Button
                            className="w-full bg-[#007AFF] hover:bg-[#0056CC]"
                            onClick={handleSave}
                            disabled={updateUser.isPending}
                        >
                            {updateUser.isPending ? (
                                <>
                                    <Loader2Icon className="mr-2 h-4 w-4 animate-spin" />
                                    保存中...
                                </>
                            ) : (
                                "保存修改"
                            )}
                        </Button>
                    </CardContent>
                </Card>

                {/* 退出登录 */}
                <Button
                    variant="outline"
                    className="w-full text-red-600 hover:text-red-600 hover:bg-red-50"
                    onClick={handleLogout}
                >
                    退出登录
                </Button>
                </div>
            </div>
        </div>
    );
}