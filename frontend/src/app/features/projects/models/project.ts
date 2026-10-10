export interface ProjectDto {
  readonly id: string;
  readonly name: string;
  readonly description: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}
export interface CreateProjectRequest {
  readonly name: string;
  readonly description?: string | null;
}
