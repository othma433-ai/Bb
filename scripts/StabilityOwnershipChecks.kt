import com.althmany.extractor.engine.RuntimeOperation
import com.althmany.extractor.engine.RuntimeOperationCoordinator

fun main() {
    RuntimeOperationCoordinator.resetForTests()
    check(RuntimeOperationCoordinator.ensureOwned(RuntimeOperation.SENDER))
    check(RuntimeOperationCoordinator.ensureOwned(RuntimeOperation.SENDER))
    check(!RuntimeOperationCoordinator.tryAcquire(RuntimeOperation.EXTRACTION))
    RuntimeOperationCoordinator.release(RuntimeOperation.SENDER)
    check(RuntimeOperationCoordinator.tryAcquire(RuntimeOperation.EXTRACTION))
    RuntimeOperationCoordinator.release(RuntimeOperation.EXTRACTION)
    println("StabilityOwnershipChecks: PASS")
}
