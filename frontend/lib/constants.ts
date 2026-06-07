import { generateDummyPassword } from "./db/utils";

export const isProductionEnvironment = process.env.NODE_ENV === "production";
export const isDevelopmentEnvironment = process.env.NODE_ENV === "development";
export const isTestEnvironment = Boolean(
  process.env.PLAYWRIGHT_TEST_BASE_URL ||
    process.env.PLAYWRIGHT ||
    process.env.CI_PLAYWRIGHT
);

export const guestRegex = /^guest-\d+$/;

export const DUMMY_PASSWORD = generateDummyPassword();

export const suggestions = [
  "帮我安排一个亲子周末，孩子在5岁，喜欢户外",
  "朋友聚会下午去哪玩？4个人想找个有趣的地方",
  "推荐一个适合情侣的周末半日游路线",
  "附近有什么好吃的餐厅？帮我找个评分高的",
];
