package org.starcoin.sirius.hub

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.map
import org.starcoin.sirius.core.*
import org.starcoin.sirius.datastore.DataStoreFactory
import org.starcoin.sirius.datastore.MapDataStoreFactory
import org.starcoin.sirius.hub.HubService.HubMaliciousFlag
import org.starcoin.sirius.protocol.Chain
import org.starcoin.sirius.protocol.ChainAccount
import org.starcoin.sirius.protocol.HubContract
import java.util.*

class HubServiceImpl<A : ChainAccount>(
    owner: A,
    chain: Chain<out ChainTransaction, out Block<out ChainTransaction>, A>,
    contract: HubContract<A>,
    dataStoreFactory: DataStoreFactory = MapDataStoreFactory()
) : HubService {

    private val hub: Hub = HubImpl(owner, chain, contract, dataStoreFactory)

    override var hubMaliciousFlag: EnumSet<HubMaliciousFlag>
        get() = hub.hubMaliciousFlag
        set(flags) {
            hub.hubMaliciousFlag = flags
        }

    override val hubInfo: HubInfo
        get() = this.hub.hubInfo

    override fun start() {
        this.hub.start()
    }

    override suspend fun registerParticipant(participant: Participant, initUpdate: Update): Update {
        return this.hub.registerParticipant(participant, initUpdate)
    }

    override suspend fun sendNewTransfer(iou: IOU) {
        this.hub.sendNewTransfer(iou)
    }

    override suspend fun receiveNewTransfer(receiverIOU: IOU) {
        this.hub.receiveNewTransfer(receiverIOU)
    }

    override suspend fun queryNewTransfer(address: Address): List<OffchainTransaction> {
        return this.hub.queryNewTransfer(address)
    }

    override suspend fun querySignedUpdate(address: Address): Update? {
        val hubAccount = this.hub.getHubAccount(address)
        return hubAccount?.update
    }

    override suspend fun querySignedUpdate(eon: Int, blockAddress: Address): Update? {
        val hubAccount = this.hub.getHubAccount(eon, blockAddress)
        return hubAccount?.update
    }

    override suspend fun getProof(address: Address): AMTreeProof? {
        return hub.getProof(address)
    }

    override suspend fun getProof(eon: Int, blockAddress: Address): AMTreeProof? {
        return hub.getProof(eon, blockAddress)
    }

    override suspend fun watch(address: Address): ReceiveChannel<HubEvent> {
        return hub.watch(address)
    }

    override suspend fun watchHubRoot(): ReceiveChannel<HubRoot> {
        return hub.watch { event -> event.type === HubEventType.NEW_HUB_ROOT }.map { it.getPayload<HubRoot>() }
    }

    override suspend fun getHubAccount(address: Address): HubAccount? {
        return this.hub.getHubAccount(address)
    }

    override suspend fun resetHubMaliciousFlag(): EnumSet<HubMaliciousFlag> {
        return this.hub.resetHubMaliciousFlag()
    }

    override fun stop() {
        this.hub.stop()
    }
}
