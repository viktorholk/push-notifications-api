import { Router } from "express";

const router = Router();

import { connect, create, getAll, getLatest } from "@/controllers/push-notification";

router.post("/", create);
router.get("/", getAll);
router.get("/latest", getLatest);
router.get("/events", connect);

export default router;
