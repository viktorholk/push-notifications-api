import Logger from "@/utils/logger";
import { Request, Response, Router } from "express";

const router = Router();

interface PushNotification {
  title: string;
  message?: string;
  url?: string
}

type ClientContext = { req: Request; res: Response };

let notifications: PushNotification[] = [];
let clients: ClientContext[] = [];

function sendNotifications(notification: PushNotification): void {
  clients.forEach(({ req, res }) => {
    Logger.info(`Sending notification to ${req.socket?.remoteAddress}`);
    const data = `data: ${JSON.stringify(notification)}\n\n`;
    res.write(data);
  });
}

// Create new notification
router.post("/", (req: Request, res: Response) => {

  const { title, message, url } = req.body;

  console.log(url)

  if (!title || title.trim() === "") {
    return res.status(400).send("'title' field is required");
  }

  const notification: PushNotification = { title, message, url };

  notifications.push(notification);
  sendNotifications(notification);

  res.sendResponse(201);
});

// Get all notifications
router.get("/", (_req: Request, res: Response) => {
  res.sendResponse(200, notifications);
});

// Get the latest notification
router.get("/latest", (_req: Request, res: Response) => {
  const latestNotification = notifications.length ? notifications[notifications.length - 1] : null;

  if (!latestNotification) {
    return res.sendResponse(404);
  }

  res.sendResponse(200, latestNotification);
});

// SSE Endpoint to listen for notifications
router.get("/events", (req: Request, res: Response) => {
  // Set headers to keep the connection open
  res.setHeader("Content-Type", "text/event-stream");
  res.setHeader("Cache-Control", "no-cache");
  res.setHeader("Connection", "keep-alive");

  Logger.info(`${req.socket?.remoteAddress} connected`);
  clients.push({ req, res });

  res.write("data: Connected\n\n");

  req.on("close", () => {
    Logger.info(`${req.socket?.remoteAddress} disconnected`);
    clients = clients.filter(client => client.req !== req);
  });
});

export default router;
