import { NextFunction, Request, Response } from "express";

import correlator from "correlation-id";
import Logger from "@/utils/logger";

export default function(req: Request, res: Response, next: NextFunction) {
    const id = correlator.getId();

    const middlewareLogic = () => {
        // Log the initial request
        Logger.request(`${req.method} ${req.path}`, req.body);

        const originalSend = res.send;
        res.send = function(body) {
            Logger.response(res.statusCode, body);
            return originalSend.call(this, body);
        };

        next();
    };

    if (id) {
        correlator.withId(id, middlewareLogic);
    } else {
        correlator.withId(middlewareLogic);
    }
}
