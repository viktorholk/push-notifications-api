import moment from "moment";
import { Request, Response } from "express";
import { v4 as uuidv4 } from "uuid";
import Logger from "@/utils/logger";
import { ClientContext, PushNotification } from "@/types";
import { loadIcon, sendNotifications } from "@/utils";
import { getDatabase } from "@/db";

const db = getDatabase();
let clients: ClientContext[] = [];

const HEARTBEAT_INTERVAL_MS = 30000;

export function addClient(req: Request, res: Response, token: string | undefined) {
  Logger.info(
    `${req.socket?.remoteAddress} connected${token ? ` with token ${token}` : ""}`,
  );
  clients.push({ req, res, token: token || "" });

  // Send periodic heartbeat comments to keep the SSE connection alive
  // through NAT tables, proxies, and carrier networks that kill idle connections
  const heartbeat = setInterval(() => {
    try {
      res.write(": heartbeat\n\n");
    } catch (e) {
      clearInterval(heartbeat);
    }
  }, HEARTBEAT_INTERVAL_MS);

  req.on("close", () => {
    clearInterval(heartbeat);
    Logger.info(`${req.socket?.remoteAddress} disconnected`);
    clients = clients.filter((client) => client.req !== req);
  });
}

export type CreateNotificationParams = {
  title: string;
  message?: string;
  url?: string;
  icon?: string;
  color?: string;
  token?: string | null;
}

export async function createNotification(params: CreateNotificationParams): Promise<void> {
  const { title, message, url, icon, color, token } = params;

  if (!title || title.trim() === "") {
    throw new Error("'title' field is required");
  }

  const notification: PushNotification = {
    id: uuidv4(),
    title,
    message,
    url,
    icon,
    color,
    token: token,
    createdAt: new Date().toISOString(),
  };

  if (icon) {
    const base64Icon = loadIcon(icon);
    if (base64Icon) {
      notification.icon = base64Icon;
    }
  }

  await db
    .insertInto("notifications")
    .values({
      id: notification.id,
      title: notification.title,
      message: notification.message || null,
      url: notification.url || null,
      icon: notification.icon || null,
      color: notification.color || null,
      token: notification.token,
      createdAt: notification.createdAt,
    })
    .execute();

  // Send the notification to all the connected clients
  sendNotifications(clients, notification);
}

export async function getAllNotifications(token: string, since?: string): Promise<PushNotification[]> {
  let query = db
    .selectFrom("notifications")
    .selectAll()
    .orderBy("createdAt", "desc");

  query = query.where((eb) =>
    eb("token", "=", token).or("token", "is", null)
  );

  const notifications = await query.execute();

  let result: PushNotification[] = notifications.map((n) => ({
    id: n.id,
    title: n.title,
    message: n.message || undefined,
    url: n.url || undefined,
    icon: n.icon || undefined,
    color: n.color || undefined,
    token: n.token,
    createdAt: n.createdAt,
  }));

  if (since) {
    result = result.filter((n) => moment(n.createdAt).isAfter(moment(since)));
  }

  return result;
}

export async function getLatestNotification(token: string): Promise<PushNotification | null> {
  let query = db
    .selectFrom("notifications")
    .selectAll()
    .orderBy("createdAt", "desc")
    .limit(1);

  query = query.where((eb) =>
    eb("token", "=", token).or("token", "is", null)
  );

  const latestNotification = await query.executeTakeFirst();

  if (!latestNotification) {
    return null;
  }

  return {
    id: latestNotification.id,
    title: latestNotification.title,
    message: latestNotification.message || undefined,
    url: latestNotification.url || undefined,
    icon: latestNotification.icon || undefined,
    color: latestNotification.color || undefined,
    token: latestNotification.token,
    createdAt: latestNotification.createdAt,
  };
}

export async function registerDevice(): Promise<string> {
  const token = uuidv4();
  await db
    .insertInto("registrations")
    .values({ token })
    .onConflict((oc) => oc.column("token").doNothing())
    .execute();

  return token;
}
