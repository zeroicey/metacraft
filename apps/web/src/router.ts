import { lazy } from "react";
import { createHashRouter } from "react-router";
import RootLayout from "@/components/layouts/root-layout";
import StandaloneLayout from "@/components/layouts/standalone-layout";

const YuanChuangPage = lazy(() => import("@/pages/yuanchuang"));
const YuanMengPage = lazy(() => import("@/pages/yuanmeng"));
const MyAppsPage = lazy(() => import("@/pages/myapps"));
// const IndexPage = lazy(() => import("@/pages/index"));
const PreviewPage = lazy(() => import("@/pages/preview"));
const NotFoundPage = lazy(() => import("@/pages/404"));
const ErrorPage = lazy(() => import("@/pages/error"));
const LoginPage = lazy(() => import("@/pages/login"));
const RegisterPage = lazy(() => import("@/pages/register"));
const ProfilePage = lazy(() => import("@/pages/profile"));
const StorePage = lazy(() => import("@/pages/store"));
const StoreDetailPage = lazy(() => import("@/pages/store-detail"));

const router = createHashRouter([
    {
        path: "/",
        Component: RootLayout,
        ErrorBoundary: ErrorPage,
        children: [
            { index: true, Component: YuanChuangPage },
            { path: "yuanchuang", Component: YuanChuangPage },
            { path: "yuanmeng", Component: YuanMengPage },
            { path: "*", Component: NotFoundPage },
        ],
    },
    {
        path: "/preview",
        Component: StandaloneLayout,
        ErrorBoundary: ErrorPage,
        children: [
            { index: true, Component: PreviewPage },
        ],
    },
    {
        path: "/myapps",
        Component: StandaloneLayout,
        ErrorBoundary: ErrorPage,
        children: [
            { index: true, Component: MyAppsPage },
        ],
    },
    {
        path: "/login",
        Component: StandaloneLayout,
        ErrorBoundary: ErrorPage,
        children: [
            { index: true, Component: LoginPage },
        ],
    },
    {
        path: "/register",
        Component: StandaloneLayout,
        ErrorBoundary: ErrorPage,
        children: [
            { index: true, Component: RegisterPage },
        ],
    },
    {
        path: "/profile",
        Component: StandaloneLayout,
        ErrorBoundary: ErrorPage,
        children: [
            { index: true, Component: ProfilePage },
        ],
    },
    {
        path: "/store",
        Component: StandaloneLayout,
        ErrorBoundary: ErrorPage,
        children: [
            { index: true, Component: StorePage },
        ],
    },
    {
        path: "/store/:id",
        Component: StandaloneLayout,
        ErrorBoundary: ErrorPage,
        children: [
            { index: true, Component: StoreDetailPage },
        ],
    },
]);

export default router;