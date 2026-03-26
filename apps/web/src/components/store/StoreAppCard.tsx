import { useNavigate } from "react-router";
import { Star } from "lucide-react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Card, CardContent } from "@/components/ui/card";
import type { StoreAppItem } from "@/api/store";
import { API_BASE_URL } from "@/lib/config";

interface StoreAppCardProps {
  app: StoreAppItem;
}

/**
 * Get the full logo URL from stored logo path
 */
function getLogoUrl(logo: string | null): string {
  if (!logo || logo.length === 0) {
    return "";
  }
  if (logo.startsWith("http://") || logo.startsWith("https://")) {
    return logo;
  }
  const dotIndex = logo.lastIndexOf(".");
  const logoUuid = dotIndex > 0 ? logo.substring(0, dotIndex) : logo;
  return `${API_BASE_URL}/api/logo/${logoUuid}`;
}

export default function StoreAppCard({ app }: StoreAppCardProps) {
  const navigate = useNavigate();
  const logoUrl = getLogoUrl(app.logo);

  return (
    <Card
      className="cursor-pointer transition-shadow hover:shadow-lg"
      onClick={() => navigate(`/store/${app.id}`)}
    >
      <CardContent className="p-4">
        <div className="flex gap-4">
          {/* Logo */}
          <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-lg bg-muted">
            {logoUrl ? (
              <img
                src={logoUrl}
                alt={app.name}
                className="h-full w-full rounded-lg object-cover"
              />
            ) : (
              <span className="text-2xl font-bold text-muted-foreground">
                {app.name.charAt(0)}
              </span>
            )}
          </div>

          <div className="flex min-w-0 flex-1 flex-col justify-between">
            {/* Name */}
            <h3 className="truncate font-medium">{app.name}</h3>

            {/* Rating */}
            <div className="flex items-center gap-1 text-sm text-muted-foreground">
              <Star className="size-4 fill-yellow-500 text-yellow-500" />
              <span>
                {app.averageRating !== null
                  ? app.averageRating.toFixed(1)
                  : "暂无"}
              </span>
              <span className="text-xs">({app.ratingCount})</span>
            </div>

            {/* Author */}
            <div className="flex items-center gap-2">
              <Avatar size="sm">
                <AvatarImage src={app.author.avatarBase64 || undefined} />
                <AvatarFallback>
                  {app.author.name.charAt(0)}
                </AvatarFallback>
              </Avatar>
              <span className="truncate text-sm text-muted-foreground">
                {app.author.name}
              </span>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}