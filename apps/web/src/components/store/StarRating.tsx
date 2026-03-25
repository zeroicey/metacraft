import { useState } from "react";
import { Star } from "lucide-react";
import { Button } from "@/components/ui/button";

interface StarRatingProps {
  initialRating?: number;
  onRate: (rating: number) => Promise<void>;
  disabled?: boolean;
}

export default function StarRating({
  initialRating,
  onRate,
  disabled = false,
}: StarRatingProps) {
  const [rating, setRating] = useState<number>(initialRating || 0);
  const [hoverRating, setHoverRating] = useState<number>(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const displayRating = hoverRating || rating;

  const handleClick = (value: number) => {
    if (disabled || isSubmitting) return;
    setRating(value);
  };

  const handleConfirm = async () => {
    if (rating === 0 || isSubmitting) return;

    setIsSubmitting(true);
    try {
      await onRate(rating);
      setSubmitted(true);
    } catch (error) {
      console.error("Failed to submit rating:", error);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (submitted) {
    return (
      <div className="flex items-center gap-2 text-green-600">
        <Star className="size-5 fill-green-600" />
        <span className="text-sm font-medium">评分成功</span>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-center gap-1">
        {[1, 2, 3, 4, 5].map((value) => (
          <button
            key={value}
            type="button"
            disabled={disabled || isSubmitting}
            className="p-0.5 transition-transform hover:scale-110 disabled:cursor-not-allowed disabled:hover:scale-100"
            onClick={() => handleClick(value)}
            onMouseEnter={() => setHoverRating(value)}
            onMouseLeave={() => setHoverRating(0)}
          >
            <Star
              className="size-6 transition-colors"
              fill={
                value <= displayRating ? "#eab308" : "transparent"
              }
              stroke={value <= displayRating ? "#eab308" : "currentColor"}
            />
          </button>
        ))}
      </div>

      {rating > 0 && !submitted && (
        <Button
          size="sm"
          onClick={handleConfirm}
          disabled={isSubmitting || disabled}
        >
          {isSubmitting ? "提交中..." : "确认"}
        </Button>
      )}
    </div>
  );
}