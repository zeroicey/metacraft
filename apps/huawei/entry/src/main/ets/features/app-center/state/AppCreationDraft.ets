@Observed
export class AppCreationDraft {
  name: string = '';
  description: string = '';

  reset(): void {
    this.name = '';
    this.description = '';
  }

  isValid(): boolean {
    return this.name.trim().length > 0;
  }

  getTrimmedName(): string {
    return this.name.trim();
  }

  getTrimmedDescription(): string {
    return this.description.trim();
  }
}