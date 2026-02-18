import dotenv from "dotenv";

dotenv.config();

export const PORT = parseInt(process.env.PORT || "3000", 10);
export const HOST = process.env.HOST || "0.0.0.0";
