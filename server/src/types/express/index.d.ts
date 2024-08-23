// Empty export statement to keep file a module

export { }

declare global {
  namespace Express {
    export interface Response {
      sendResponse: (statusCode: number, data?: any) => Response;
    }
  }
}
