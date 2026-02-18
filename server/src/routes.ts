import { Router, Request, Response } from "express";
import { tokenMiddleware } from "@/middleware/token";
import Logger from "@/utils/logger";
import {
  addClient,
  createNotification,
  getAllNotifications,
  getLatestNotification,
  registerDevice
} from "./handlers";

const router = Router();

// Register device
router.post("/register", async (req: Request, res: Response) => {
  try {
    const token = await registerDevice();
    res.json({ token });
  } catch (err) {
    Logger.error("Error registering device", err);
    res.status(500).json({ error: "Registration failed" });
  }
});

// Create notification
router.post("/notifications", tokenMiddleware, async (req: Request, res: Response) => {
  try {
    const { title, message, url, icon, color } = req.body;
    let token = (req as any).token;

    // If no token is provided, treat it as a broadcast (global notification)
    if (!token) {
      token = null;
    }

    await createNotification({
      title,
      message,
      url,
      icon,
      color,
      token
    });

    res.sendStatus(201);
  } catch (err: any) {
    if (err.message === "'title' field is required") {
        return res.status(400).send(err.message);
    }
    Logger.error("Error creating notification", err);
    res.status(500).send("Internal Server Error");
  }
});

// Get all notifications
router.get("/notifications", tokenMiddleware, async (req: Request, res: Response) => {
  const token = (req as any).token;
  const since = req.query.since as string;

  if (!token) {
    return res.status(401).json({ error: "Access token is required" });
  }

  try {
    const notifications = await getAllNotifications(token, since);
    res.json(notifications);
  } catch (err) {
    Logger.error("Error fetching notifications", err);
    res.status(500).send("Internal Server Error");
  }
});

// Get latest notification
router.get("/latest", tokenMiddleware, async (req: Request, res: Response) => {
  const token = (req as any).token;

  if (!token) {
    return res.status(401).json({ error: "Access token is required" });
  }

  try {
    const notification = await getLatestNotification(token);
    
    if (!notification) {
      return res.sendStatus(404);
    }
    
    res.json(notification);
  } catch (err) {
    Logger.error("Error fetching latest notification", err);
    res.status(500).send("Internal Server Error");
  }
});

// Connect to SSE stream
router.get("/events", async (req: Request, res: Response) => {
  // Set headers to keep the connection open
  res.setHeader("Content-Type", "text/event-stream");
  res.setHeader("Cache-Control", "no-cache");
  res.setHeader("Connection", "keep-alive");

  const token = req.query.token as string;

  addClient(req, res, token);

  res.write("data: Connected\n\n");
});

export default router;
