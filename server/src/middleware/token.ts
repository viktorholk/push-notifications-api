import { Request, Response, NextFunction } from "express";

export function tokenMiddleware(req: Request, res: Response, next: NextFunction) {
  const token =
    (req.query.token as string) || 
    (req.headers["x-access-token"] as string) ||
    (req.body && req.body.token);

  if (token) {
    (req as any).token = token;
  }
  
  next();
}
