import { Request, Response } from "express";

import { PushNotification } from "@/interfaces";
import { loadIcon } from "@/utils/icon-handler";

import Logger from "@/utils/logger";

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

export function connect(req: Request, res: Response) {
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

}

export function create(req: Request, res: Response) {
  const { title, message, url, icon, color } = req.body;

  if (!title || title.trim() === "") {
    return res.status(400).send("'title' field is required");
  }

  const notification: PushNotification = { title, message, url, icon, color };

  if (icon) {
    const base64Icon = loadIcon(icon);
    if (base64Icon) {
      notification.icon = base64Icon;
    }
  }

  notifications.push(notification);
  sendNotifications(notification);

  res.sendResponse(201);

}

export function getAll(_req: Request, res: Response) {
  res.sendResponse(200, notifications);
}

export function getLatest(_req: Request, res: Response) {
  const latestNotification = notifications.length ? notifications[notifications.length - 1] : null;

  if (!latestNotification) {
    return res.sendResponse(404);
  }

  res.sendResponse(200, latestNotification);

}
