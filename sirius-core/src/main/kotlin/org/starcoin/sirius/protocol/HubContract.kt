package org.starcoin.sirius.protocol

import org.starcoin.sirius.chain.ChainStrategy
import org.starcoin.sirius.core.*
import kotlin.reflect.KClass

open class FunctionSignature(val value: ByteArray) {

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FunctionSignature) return false

        if (!value.contentEquals(other.value)) return false

        return true
    }

    final override fun hashCode(): Int {
        return value.contentHashCode()
    }
}

sealed class ContractFunction<S : SiriusObject>(val name: String, val inputClass: KClass<S>) {
    val signature by lazy { ChainStrategy.signature(this) }

    companion object {
        val functions by lazy {
            ContractFunction::class.sealedSubclasses.map { it.objectInstance!! }.map { it.signature to it }.toMap()
        }
    }

    fun decode(data: ByteArray?): S? {
        return data?.let { ChainStrategy.decode(this, data) }
    }

    fun encode(input: S): ByteArray {
        return ChainStrategy.encode(this, input)
    }

    override fun toString(): String {
        return name
    }

}

object CommitFunction : ContractFunction<HubRoot>("commit", HubRoot::class)
object InitiateWithdrawalFunction : ContractFunction<Withdrawal>("initiateWithdrawal", Withdrawal::class)
object CancelWithdrawalFunction : ContractFunction<CancelWithdrawal>("cancelWithdrawal", CancelWithdrawal::class)
object OpenBalanceUpdateChallengeFunction :
    ContractFunction<BalanceUpdateProof>("openBalanceUpdateChallenge", BalanceUpdateProof::class)

object CloseBalanceUpdateChallengeFunction :
    ContractFunction<CloseBalanceUpdateChallenge>("closeBalanceUpdateChallenge", CloseBalanceUpdateChallenge::class)

object OpenTransferDeliveryChallengeFunction :
    ContractFunction<TransferDeliveryChallenge>("openTransferDeliveryChallenge", TransferDeliveryChallenge::class)

object CloseTransferDeliveryChallengeFunction :
    ContractFunction<CloseTransferDeliveryChallenge>(
        "closeTransferDeliveryChallenge",
        CloseTransferDeliveryChallenge::class
    )

object RecoverFundsFunction : ContractFunction<AMTreeProof>("recoverFundsFunction", AMTreeProof::class)

abstract class HubContract<A : ChainAccount> {

    abstract val contractAddress: Address

    fun queryHubInfo(account: A): ContractHubInfo {
        return this.queryContractFunction(account, "queryHubInfo", ContractHubInfo::class)!!
    }

    fun getLatestRoot(account: A): HubRoot? {
        //TODO check has value.
        return this.queryContractFunction(account, "queryLatestRoot", HubRoot::class)
    }

    fun queryCurrentBalanceUpdateChallenge(account: A, address: Address): BalanceUpdateProof? {
        return this.queryContractFunction(
            account,
            "queryCurrentBalanceUpdateChallenge",
            BalanceUpdateProof::class,
            address
        )
    }

    fun queryCurrentTransferDeliveryChallenge(account: A, address: Address): TransferDeliveryChallenge? {
        return this.queryContractFunction(
            account,
            "queryCurrentTransferDeliveryChallenge",
            TransferDeliveryChallenge::class,
            address
        )
    }

    fun queryWithdrawalStatus(account: A): WithdrawalStatus? {
        return this.queryContractFunction(
            account,
            "queryWithdrawal",
            WithdrawalStatus::class
        )
    }

    fun getCurrentEon(account: A): Int {
        return this.queryContractFunction(account, "queryCurrentEon", Int::class)!!
    }

    fun isRecoveryMode(account: A): Boolean {
        return this.queryContractFunction(account, "queryRecoveryMode", Boolean::class)!!
    }

    fun initiateWithdrawal(account: A, input: Withdrawal): TxDeferred {
        return this.executeContractFunction(account, InitiateWithdrawalFunction, input)
    }

    fun cancelWithdrawal(account: A, input: CancelWithdrawal): TxDeferred {
        return this.executeContractFunction(account, CancelWithdrawalFunction, input)
    }

    fun openBalanceUpdateChallenge(account: A, input: BalanceUpdateProof): TxDeferred {
        return this.executeContractFunction(
            account,
            OpenBalanceUpdateChallengeFunction, input
        )
    }

    fun closeBalanceUpdateChallenge(account: A, input: CloseBalanceUpdateChallenge): TxDeferred {
        return this.executeContractFunction(
            account,
            CloseBalanceUpdateChallengeFunction, input
        )
    }

    fun commit(account: A, input: HubRoot): TxDeferred {
        return this.executeContractFunction(account, CommitFunction, input)
    }

    fun openTransferDeliveryChallenge(account: A, input: TransferDeliveryChallenge): TxDeferred {
        return this.executeContractFunction(
            account,
            OpenTransferDeliveryChallengeFunction, input
        )
    }

    fun closeTransferDeliveryChallenge(account: A, input: CloseTransferDeliveryChallenge): TxDeferred {
        return this.executeContractFunction(
            account,
            CloseTransferDeliveryChallengeFunction, input
        )
    }

    fun recoverFunds(account: A, input: AMTreeProof): TxDeferred {
        return this.executeContractFunction(account, RecoverFundsFunction, input)
    }

    abstract fun <S : SiriusObject> executeContractFunction(
        account: A,
        function: ContractFunction<S>,
        arguments: S
    ): TxDeferred

    abstract fun <S : Any> queryContractFunction(
        account: A,
        functionName: String,
        clazz: KClass<S>,
        vararg args: Any
    ): S?

    abstract fun setHubIp(account: A, ip: String)
}
