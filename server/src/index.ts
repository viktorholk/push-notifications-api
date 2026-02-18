import dotenv from "dotenv";
dotenv.config();

import express from "express";
import cors from "cors";
import ip from "ip";

import Logger from "@/utils/logger";
import LoggerMiddleware from "@/middleware/logger";
import router from "./routes";
import { initDatabase } from "./db";
import { PORT, HOST } from "./config";

const app = express();

app.use(cors());
app.use(express.json());
app.use(LoggerMiddleware);

app.use("/", router);

const startServer = async () => {
    try {
        await initDatabase();

        app.listen(PORT, HOST, () => {
          Logger.info(
            `Server is running on http://${HOST === "0.0.0.0" ? ip.address() : HOST}:${PORT}`,
          );
        });
    } catch (err) {
        Logger.error("Failed to start server", err);
        process.exit(1);
    }
};

startServer();

export default app;
