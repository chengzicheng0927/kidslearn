/**
 * API 配置
 * 控制前端数据来源：Mock 或真实后端
 *
 * 使用方式：
 * - 开发调试：USE_MOCK = true（默认）
 * - 切换到后端：USE_MOCK = false，并配置 API_BASE_URL
 */

export const API_CONFIG = {
  /** 是否使用 Mock 数据（开发模式） */
  USE_MOCK: false,

  /** 真实后端 API 地址（生产模式） */
  API_BASE_URL: 'http://localhost:8080',

  /** API 请求超时时间（毫秒） */
  TIMEOUT: 30_000,

  /** 是否开启请求日志 */
  DEBUG: true,
} as const

export type ApiMode = 'mock' | 'real'

/**
 * 获取当前 API 模式
 */
export function getApiMode(): ApiMode {
  return API_CONFIG.USE_MOCK ? 'mock' : 'real'
}
