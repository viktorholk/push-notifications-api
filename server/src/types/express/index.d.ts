// Empty export statement to keep file a module
export { }

declare global {
    namespace Express {
        export interface Response {
            // used in middleware
            sendResponse: (statusCode: number, data?: any) => Response;
        }
    }
}
