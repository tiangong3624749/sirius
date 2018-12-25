package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.apache.commons.lang3.RandomUtils
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema

//TODO ensure timestamp
@Serializable
@ProtobufSchema(Starcoin.OffchainTransactionData::class)
data class OffchainTransactionData(
    @SerialId(1)
    val eon: Int = 0,
    @SerialId(2)
    val from: Address,
    @SerialId(3)
    val to: Address,
    @SerialId(4)
    val amount: Long = 0,
    @SerialId(5)
    val timestamp: Long = System.currentTimeMillis()
) : SiriusObject() {

    companion object :
        SiriusObjectCompanion<OffchainTransactionData, Starcoin.OffchainTransactionData>(
            OffchainTransactionData::class
        ) {
        override fun mock(): OffchainTransactionData {
            return OffchainTransactionData(
                RandomUtils.nextInt(),
                Address.random(),
                Address.random(),
                RandomUtils.nextLong(),
                RandomUtils.nextLong()
            )
        }

        override fun parseFromProtoMessage(proto: Starcoin.OffchainTransactionData): OffchainTransactionData {
            return OffchainTransactionData(
                proto.eon,
                Address.wrap(proto.from),
                Address.wrap(proto.to),
                proto.amount,
                proto.timestamp
            )
        }

        override fun toProtoMessage(obj: OffchainTransactionData): Starcoin.OffchainTransactionData {
            return Starcoin.OffchainTransactionData.newBuilder()
                .setEon(obj.eon)
                .setFrom(obj.from.toByteString())
                .setTo(obj.to.toByteString())
                .setAmount(obj.amount)
                .setTimestamp(obj.timestamp).build()
        }
    }
}