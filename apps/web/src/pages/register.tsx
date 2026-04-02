import { useState } from "react";
import { useNavigate, Link } from "react-router";
import { useAuthStore } from "@/stores/auth-store";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Loader2Icon, EyeIcon, EyeOffIcon } from "lucide-react";
import { toast } from "sonner";

export default function RegisterPage() {
    const navigate = useNavigate();
    const { register, isLoading, error } = useAuthStore();
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [name, setName] = useState("");
    const [bio, setBio] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [passwordError, setPasswordError] = useState("");

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setPasswordError("");

        if (password !== confirmPassword) {
            setPasswordError("两次输入的密码不一致");
            return;
        }

        try {
            await register({ email, password, name, bio });
            toast.success("注册成功");
            navigate("/");
        } catch {
            // Error is handled in store
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-b from-[#F0F4F8] via-white to-[#F0F4F8] px-4 py-8">
            <Card className="w-full max-w-sm rounded-2xl shadow-lg border-[#E5E7EB]">
                <CardHeader className="text-center pb-2">
                    <div className="flex justify-center mb-2">
                        <div className="w-16 h-16 rounded-2xl flex items-center justify-center shadow-md overflow-hidden">
                            <img src="/logo.png" alt="logo" className="w-full h-full object-cover" />
                        </div>
                    </div>
                    <CardTitle className="text-2xl">注册元创账号</CardTitle>
                    <CardDescription>创建账号开始创建应用</CardDescription>
                </CardHeader>
                <CardContent>
                    <form onSubmit={handleSubmit} className="space-y-3">
                        <div className="space-y-2">
                            <Input
                                type="email"
                                placeholder="邮箱"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                required
                                disabled={isLoading}
                                className="rounded-xl"
                            />
                        </div>
                        <div className="space-y-2">
                            <Input
                                type="text"
                                placeholder="姓名"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                required
                                disabled={isLoading}
                                className="rounded-xl"
                            />
                        </div>
                        <div className="space-y-2 relative">
                            <Input
                                type={showPassword ? "text" : "password"}
                                placeholder="密码"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                                disabled={isLoading}
                                className="rounded-xl"
                            />
                            <button
                                type="button"
                                onClick={() => setShowPassword(!showPassword)}
                                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                            >
                                {showPassword ? (
                                    <EyeOffIcon className="h-4 w-4" />
                                ) : (
                                    <EyeIcon className="h-4 w-4" />
                                )}
                            </button>
                        </div>
                        <div className="space-y-2">
                            <Input
                                type={showPassword ? "text" : "password"}
                                placeholder="确认密码"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                required
                                disabled={isLoading}
                                className="rounded-xl"
                            />
                        </div>
                        <div className="space-y-2">
                            <Textarea
                                placeholder="简介"
                                value={bio}
                                onChange={(e) => setBio(e.target.value)}
                                required
                                disabled={isLoading}
                                rows={3}
                                className="rounded-xl resize-none"
                            />
                        </div>

                        {(error || passwordError) && (
                            <p className="text-sm text-red-500 text-center">{error || passwordError}</p>
                        )}

                        <Button type="submit" className="w-full bg-[#007AFF] hover:bg-[#0056CC] rounded-xl" disabled={isLoading}>
                            {isLoading ? (
                                <>
                                    <Loader2Icon className="mr-2 h-4 w-4 animate-spin" />
                                    注册中...
                                </>
                            ) : (
                                "注册"
                            )}
                        </Button>
                    </form>

                    <p className="mt-4 text-center text-sm text-gray-600">
                        已有账号？{" "}
                        <Link to="/login" className="text-[#007AFF] hover:underline font-medium">
                            去登录
                        </Link>
                    </p>
                </CardContent>
            </Card>
        </div>
    );
}