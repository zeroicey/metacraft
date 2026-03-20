import { lazy } from "react";
import { createBrowserRouter } from "react-router";
import RootLayout from "@/components/layouts/root-layout";
import PreviewLayout from "@/components/layouts/preview-layout";

const YuanChuangPage = lazy(() => import("@/pages/yuanchuang"));
const YuanMengPage = lazy(() => import("@/pages/yuanmeng"));
const IndexPage = lazy(() => import("@/pages/index"));
const PreviewPage = lazy(() => import("@/pages/preview"));
const NotFoundPage = lazy(() => import("@/pages/404"));
const ErrorPage = lazy(() => import("@/pages/error"));

const router = createBrowserRouter([
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
        Component: PreviewLayout,
        ErrorBoundary: ErrorPage,
        children: [
            { index: true, Component: PreviewPage },
        ],
    },
]);

export default router;