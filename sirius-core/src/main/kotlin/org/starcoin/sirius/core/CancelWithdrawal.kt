package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.serialization.ProtobufSchema

@ProtobufSchema(Starcoin.CancelWithdrawal::class)
@Serializable
data class CancelWithdrawal(
    @SerialId(1)
    var addr: Address = Address.DUMMY_ADDRESS,
    @SerialId(2)
    var update: Update = Update.DUMMY_UPDATE,
    @SerialId(3)
    var path: AMTreePath = AMTreePath.DUMMY_PATH
) : SiriusObject() {

    companion object :
        SiriusObjectCompanion<CancelWithdrawal, Starcoin.CancelWithdrawal>(CancelWithdrawal::class) {

        var DUMMY_CANCEL_WITHDRAWAL = CancelWithdrawal()

        override fun mock(): CancelWithdrawal {
            return CancelWithdrawal(Address.random(), Update.mock(), AMTreePath.mock())
        }
    }
}
