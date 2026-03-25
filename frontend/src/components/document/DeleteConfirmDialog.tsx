"use client";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";

interface DeleteConfirmDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => void;
  isDeleting: boolean;
}

export default function DeleteConfirmDialog({
  open,
  onOpenChange,
  onConfirm,
  isDeleting,
}: DeleteConfirmDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>문서 삭제</DialogTitle>
          <DialogDescription>
            이 문서를 삭제하시겠습니까? 삭제된 문서는 복구할 수 없으며,
            공유 URL도 더 이상 접근할 수 없게 됩니다.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter className="mt-4">
          <Button
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={isDeleting}
          >
            취소
          </Button>
          <Button
            variant="destructive"
            onClick={onConfirm}
            disabled={isDeleting}
          >
            {isDeleting ? "삭제 중..." : "삭제"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
