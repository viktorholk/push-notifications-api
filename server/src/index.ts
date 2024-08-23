import dotenv from 'dotenv';
dotenv.config();

import express from 'express';
import cors from "cors";

import ip from "ip";

import Logger from "@/utils/logger";

import LoggerMiddleware from "@/middleware/logger";

import Routes from "@/routes";

async function main() {
  const app = express();

  const host = process.env.HOST || "0.0.0.0";
  const port = parseInt(process.env.PORT || "3000", 10);

  app.use(cors());
  app.use(express.json());
  app.use(LoggerMiddleware);

  app.use(Routes)

  app.listen(port, host, () => {
    Logger.info(`Server is running on http://${host === "0.0.0.0" ? ip.address() : host}:${port}`);
  });
}

main();
