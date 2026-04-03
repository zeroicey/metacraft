import { useState } from "react";
import { useNavigate, Link } from "react-router";
import { useAuthStore } from "@/stores/auth-store";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Loader2Icon } from "lucide-react";
import { toast } from "sonner";

export default function LoginPage() {
    const navigate = useNavigate();
    const { login, isLoading, error } = useAuthStore();
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            await login(email, password);
            toast.success("登录成功");
            navigate("/");
        } catch {
            // Error is handled in store
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-b from-[#F0F4F8] via-white to-[#F0F4F8] px-4">
            <Card className="w-full max-w-sm rounded-2xl shadow-lg border-[#E5E7EB]">
                <CardHeader className="text-center pb-2">
                    <div className="flex justify-center mb-2">
                        <img src="/logo.png" alt="Logo" className="w-16 h-16 p-1" />
                    </div>
                    <CardTitle className="text-2xl">欢迎来到元创</CardTitle>
                    <CardDescription>登录您的账号开始创建应用</CardDescription>
                </CardHeader>
                <CardContent>
                    <form onSubmit={handleSubmit} className="space-y-4">
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
                                type="password"
                                placeholder="密码"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                                disabled={isLoading}
                                className="rounded-xl"
                            />
                        </div>

                        {error && (
                            <p className="text-sm text-red-500 text-center">{error}</p>
                        )}

                        <Button type="submit" className="w-full bg-[#007AFF] hover:bg-[#0056CC] rounded-xl" disabled={isLoading}>
                            {isLoading ? (
                                <>
                                    <Loader2Icon className="mr-2 h-4 w-4 animate-spin" />
                                    登录中...
                                </>
                            ) : (
                                "登录"
                            )}
                        </Button>
                    </form>

                    <p className="mt-4 text-center text-sm text-gray-600">
                        还没有账号？{" "}
                        <Link to="/register" className="text-[#007AFF] hover:underline font-medium">
                            去注册
                        </Link>
                    </p>
                </CardContent>
            </Card>
        </div>
    );
}