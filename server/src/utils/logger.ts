import correlationId from "correlation-id";
import moment from "moment";
import _ from "lodash";

enum LogLevel {
  DEBUG,
  INFO,
  WARN,
  ERROR,
}

enum LogType {
  REQUEST,
  RESPONSE
}

export default class Logger {
  static debug(message: any, data?: any) {
    Logger.log(LogLevel.DEBUG, message, data, "\x1b[37m");
  }

  static info(message: any, data?: any) {
    Logger.log(LogLevel.INFO, message, data, "\x1b[34m");
  }

  static warn(message: any, data?: any) {
    Logger.log(LogLevel.WARN, message, data, "\x1b[33m");
  }

  static error(message: any, data?: any) {
    Logger.log(LogLevel.ERROR, message, data, "\x1b[31m");
  }

  static request(message: any, data?: any) {
    Logger.log(LogLevel.INFO, message, data, "\x1b[34m", LogType.REQUEST);
  }

  static response(message: any, data?: any) {
    Logger.log(LogLevel.INFO, message, data, "\x1b[34m", LogType.RESPONSE);
  }

  private static log(
    logLevel: LogLevel,
    message: any,
    data?: any,
    levelColor?: string,
    logType?: LogType,
  ) {
    const id = correlationId.getId();

    const formatted_id = id ? `(${id}) ` : "";

    let prefix = `[${moment().format(
      "D/M/YYYY HH:mm:ss"
    )}] ${formatted_id}${levelColor}${LogLevel[logLevel]}\x1b[0m`;

    if (logType !== undefined) {
      prefix += ` [${LogType[logType]}]`;
    }

    console.log(prefix, message);
    if (!_.isEmpty(data))
      console.log(prefix, JSON.stringify(data));
  }
}
