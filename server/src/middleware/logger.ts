import { NextFunction, Request, Response } from "express";

import correlator from "correlation-id";
import Logger from "@/utils/logger";

export default function(req: Request, res: Response, next: NextFunction) {
    res.sendResponse = function(
        statusCode: number,
        data?: any
    ): Response {
        if (typeof data === "string") {
            data = {
                message: data,
            };
        }
        Logger.response(statusCode, data);

        if (data)
            return this.status(statusCode).json(data);
        else
            return this.sendStatus(statusCode);

    };

    const id = correlator.getId();

    if (id) {
        correlator.withId(id, () => {
            next();
        });
    } else {
        correlator.withId(() => {
            // Log the initial request
            Logger.request(`${req.method} ${req.path}`, req.body);
            next();
        });
    }
}
