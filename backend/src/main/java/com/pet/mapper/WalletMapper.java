package com.pet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.entity.Wallet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 钱包数据层。
 * <p>
 * 所有金额变动都用<b>带余额条件的原子 UPDATE</b> 完成（{@code balance = balance - x WHERE balance >= x}），
 * 把防透支交给数据库的行锁与条件判断，而不是「先查余额再比较再更新」——后者在并发下必然透支。
 * 返回 0 即表示条件不满足（余额/冻结额不足），调用方转成业务异常。
 * <p>
 * 手写 SQL 绕过了逻辑删除拦截器与 MetaObjectHandler，每条都必须自带 {@code deleted = 0} 与 {@code update_time = NOW()}。
 */
@Mapper
public interface WalletMapper extends BaseMapper<Wallet> {

    /** 充值：增加可用余额。 */
    @Update("UPDATE t_wallet SET balance = balance + #{amount}, update_time = NOW() "
            + "WHERE user_id = #{userId} AND deleted = 0")
    int recharge(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /** 支付下单：可用余额转入冻结（平台担保）。返回 0 = 余额不足。 */
    @Update("UPDATE t_wallet SET balance = balance - #{amount}, frozen = frozen + #{amount}, update_time = NOW() "
            + "WHERE user_id = #{userId} AND balance >= #{amount} AND deleted = 0")
    int freeze(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /** 取消退款：冻结金额退回可用余额。返回 0 = 冻结额不足（说明已退过）。 */
    @Update("UPDATE t_wallet SET frozen = frozen - #{amount}, balance = balance + #{amount}, update_time = NOW() "
            + "WHERE user_id = #{userId} AND frozen >= #{amount} AND deleted = 0")
    int unfreezeToBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /** 验收结算：担保资金从下单用户的冻结额中划出，不再回到其余额。 */
    @Update("UPDATE t_wallet SET frozen = frozen - #{amount}, update_time = NOW() "
            + "WHERE user_id = #{userId} AND frozen >= #{amount} AND deleted = 0")
    int deductFrozen(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /** 收入入账：可用余额与累计收入同时增加（接单员佣金、平台抽成都走这里）。 */
    @Update("UPDATE t_wallet SET balance = balance + #{amount}, total_income = total_income + #{amount}, update_time = NOW() "
            + "WHERE user_id = #{userId} AND deleted = 0")
    int addIncome(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /** 提现：返回 0 = 余额不足。累计收入不因提现减少。 */
    @Update("UPDATE t_wallet SET balance = balance - #{amount}, update_time = NOW() "
            + "WHERE user_id = #{userId} AND balance >= #{amount} AND deleted = 0")
    int withdraw(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 读取当前可用余额，用于写流水的 balance_after。
     * <p>
     * 必须与前面的 UPDATE 处于同一事务：UPDATE 已对该行加了排他锁并保持到提交，
     * 因此这里读到的一定是本次变动后的值，不会被并发事务插队。
     */
    @Select("SELECT balance FROM t_wallet WHERE user_id = #{userId} AND deleted = 0")
    BigDecimal selectBalance(@Param("userId") Long userId);
}
