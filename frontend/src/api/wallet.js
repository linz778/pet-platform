import request from './request'

/** 我的钱包，返回 { balance, frozen, totalIncome } */
export function getMyWallet() {
  return request.get('/wallet/me')
}

/**
 * 我的流水分页。
 * @param {{page?:number,size?:number,type?:number}} params type 为空表示全部
 */
export function pageTransactions(params) {
  return request.get('/wallet/transaction/page', { params })
}

/** 充值（模拟通道，即时到账），返回变动后的钱包 */
export function recharge(amount) {
  return request.post('/wallet/recharge', { amount })
}

/** 提现（模拟通道，即时成功），返回变动后的钱包 */
export function withdraw(amount) {
  return request.post('/wallet/withdraw', { amount })
}
