package com.scaliaos.app.models

import zio.json._

/**
 * Domain models for blockchain operations in the ScaliaOS platform.
 * 
 * This object contains all data models related to blockchain interactions, including:
 * - Transaction request models for submitting on-chain operations
 * - Transaction status tracking through the execution lifecycle
 * - Transaction state machine definitions
 * - Error types for blockchain-specific failures
 * 
 * These models provide a blockchain-agnostic abstraction layer, supporting
 * multiple networks (Ethereum, Solana, Base, Arbitrum, etc.) through a
 * unified interface.
 * 
 * All models include ZIO JSON encoders/decoders for serialization.
 */
object BlockchainModels {

  // ==================== Transaction Request Models ====================

  /**
   * Request model for submitting a blockchain transaction through an agent.
   * 
   * This is the primary interface for agents to request on-chain operations.
   * The model is intentionally flexible to support different blockchain networks
   * and action types through the params field.
   * 
   * Flow:
   * 1. Agent generates BlockchainTransactionRequest based on its logic
   * 2. Request is submitted to BlockchainService
   * 3. Service validates, queues, and executes the transaction
   * 4. Status updates are tracked via the returned requestId
   * 
   * @param agentId The ID of the agent requesting this transaction.
   *                Used for tracking, authorization, and auditing.
   * @param action The type of blockchain operation to perform.
   *               Common actions:
   *               - "swap": Token exchange on DEX
   *               - "transfer": Send tokens to an address
   *               - "approve": Grant token spending approval
   *               - "stake": Stake tokens in a protocol
   *               - "unstake": Withdraw staked tokens
   *               - "provide_liquidity": Add liquidity to a pool
   *               - "remove_liquidity": Withdraw liquidity from a pool
   * @param chain The target blockchain network.
   *              Examples: "ethereum", "base", "arbitrum", "optimism", "polygon", "solana"
   *              Case-insensitive for flexibility.
   * @param params Action-specific parameters as a flexible key-value map.
   *               Examples:
   *               - Swap: {"from_token": "USDC", "to_token": "ETH", "amount": "100"}
   *               - Transfer: {"to_address": "0x...", "token": "USDC", "amount": "50"}
   *               - Approve: {"spender": "0x...", "token": "DAI", "amount": "unlimited"}
   *               All numeric values are strings to preserve precision.
   * @param priority Transaction priority level affecting gas price and confirmation speed.
   *                 - "low": Slower, cheaper (5-30 min)
   *                 - "medium": Normal speed (1-5 min) - DEFAULT
   *                 - "high": Faster, more expensive (30 sec - 2 min)
   *                 - "urgent": Maximum speed (<30 sec), highest cost
   * @param maxGasPrice Optional maximum gas price willing to pay (in Gwei or native units).
   *                    Transaction will not be submitted if gas exceeds this limit.
   *                    Helps prevent unexpected high costs during network congestion.
   * @param metadata Optional additional context about the transaction.
   *                 Can include:
   *                 - "user_id": End user who initiated this
   *                 - "session_id": Related session for tracking
   *                 - "reason": Human-readable explanation
   *                 - "tags": Comma-separated tags for categorization
   */
  case class BlockchainTransactionRequest(
    agentId: String,
    action: String,
    chain: String,
    params: Map[String, String],
    priority: String = "medium",
    maxGasPrice: Option[String] = None,
    metadata: Option[Map[String, String]] = None
  )

  object BlockchainTransactionRequest {
    implicit val decoder: JsonDecoder[BlockchainTransactionRequest] =
      DeriveJsonDecoder.gen[BlockchainTransactionRequest]
    implicit val encoder: JsonEncoder[BlockchainTransactionRequest] =
      DeriveJsonEncoder.gen[BlockchainTransactionRequest]
  }

  // ==================== Transaction Status Models ====================

  /**
   * Sealed trait representing the state of a blockchain transaction.
   * 
   * Transaction Lifecycle:
   * 
   * Happy Path:
   * Queued → Validating → Approved → Submitting → Pending → Confirmed → Finalized
   * 
   * Error Paths:
   * - Queued/Validating → Failed (validation errors)
   * - Queued → Cancelled (user/system cancellation)
   * - Submitting/Pending → Failed (network errors, insufficient gas)
   * 
   * Terminal States:
   * - Finalized: Transaction is permanently on-chain (irreversible)
   * - Failed: Transaction encountered an error and will not proceed
   * - Cancelled: Transaction was cancelled before submission
   * 
   * Using a sealed trait ensures exhaustive pattern matching and
   * compile-time safety when handling transaction states.
   */
  sealed trait TransactionState
  
  object TransactionState {
    /**
     * Initial state when transaction is received but not yet validated.
     * Transaction is in queue waiting for processing.
     */
    case object Queued extends TransactionState
    
    /**
     * Transaction is being validated against business rules and constraints.
     * Checks include: sufficient balance, valid addresses, parameter validation.
     */
    case object Validating extends TransactionState
    
    /**
     * Transaction passed validation and is approved for submission.
     * May be waiting for user confirmation or optimal gas conditions.
     */
    case object Approved extends TransactionState
    
    /**
     * Transaction is actively being submitted to the blockchain network.
     * Creating and signing the raw transaction, sending to RPC endpoint.
     */
    case object Submitting extends TransactionState
    
    /**
     * Transaction is in the blockchain's mempool/pending pool.
     * Waiting to be included in a block by miners/validators.
     * Has a transaction hash but no block confirmation yet.
     */
    case object Pending extends TransactionState
    
    /**
     * Transaction has been included in a block (1+ confirmations).
     * Generally safe to consider successful, but not yet finalized.
     */
    case object Confirmed extends TransactionState
    
    /**
     * Transaction is permanently settled on-chain (typically 12+ confirmations).
     * Irreversible under normal circumstances. Terminal success state.
     */
    case object Finalized extends TransactionState
    
    /**
     * Transaction encountered an error and will not proceed further.
     * Could be due to: validation failure, network error, insufficient gas,
     * reverted smart contract, or timeout. Terminal failure state.
     */
    case object Failed extends TransactionState
    
    /**
     * Transaction was cancelled before being submitted to the blockchain.
     * Only possible in Queued/Validating states. Terminal state.
     */
    case object Cancelled extends TransactionState

    /**
     * Parses a string into a TransactionState.
     * Case-insensitive for flexibility. Defaults to Queued for unknown values.
     * 
     * @param s The state string to parse
     * @return The corresponding TransactionState
     */
    def fromString(s: String): TransactionState = s.toLowerCase match {
      case "queued"     => Queued
      case "validating" => Validating
      case "approved"   => Approved
      case "submitting" => Submitting
      case "pending"    => Pending
      case "confirmed"  => Confirmed
      case "finalized"  => Finalized
      case "failed"     => Failed
      case "cancelled"  => Cancelled
      case _            => Queued  // Safe default for unknown states
    }

    /**
     * Converts a TransactionState to its string representation.
     * Uses lowercase for consistency with API conventions.
     * 
     * @param state The state to convert
     * @return The string representation
     */
    def toString(state: TransactionState): String = state match {
      case Queued     => "queued"
      case Validating => "validating"
      case Approved   => "approved"
      case Submitting => "submitting"
      case Pending    => "pending"
      case Confirmed  => "confirmed"
      case Finalized  => "finalized"
      case Failed     => "failed"
      case Cancelled  => "cancelled"
    }

    /**
     * JSON encoder for TransactionState.
     * Serializes to lowercase string representation.
     */
    implicit val encoder: JsonEncoder[TransactionState] =
      JsonEncoder[String].contramap(toString)
      
    /**
     * JSON decoder for TransactionState.
     * Parses from string, case-insensitive with safe default.
     */
    implicit val decoder: JsonDecoder[TransactionState] =
      JsonDecoder[String].map(fromString)
  }

  /**
   * Comprehensive status information for a blockchain transaction.
   * 
   * Provides complete tracking data for a transaction from submission
   * through finalization, including timing, gas costs, and error details.
   * 
   * This model is returned by:
   * - getTransactionStatus (single transaction query)
   * - listPendingTransactions (bulk query)
   * - Transaction status webhooks/events
   * 
   * @param requestId Unique identifier for this transaction request.
   *                  Used to query status and correlate with original request.
   * @param agentId The agent that requested this transaction.
   * @param action The blockchain action being performed (swap, transfer, etc.).
   * @param chain The blockchain network (ethereum, solana, etc.).
   * @param state Current state in the transaction lifecycle.
   * @param txHash On-chain transaction hash (available once submitted).
   *               Can be used to view transaction on block explorer.
   *               Format varies by chain (0x... for EVM, base58 for Solana).
   * @param blockNumber Block number where transaction was included (if confirmed).
   * @param gasUsed Actual gas consumed by the transaction (once confirmed).
   *                String to preserve precision. Units are chain-specific.
   * @param gasPrice Gas price paid for the transaction (in Gwei or native units).
   *                 String to preserve precision.
   * @param error Error message if transaction failed.
   *              Contains human-readable explanation of failure reason.
   * @param createdAt Timestamp when transaction was first queued (Unix milliseconds).
   * @param updatedAt Timestamp of last status update (Unix milliseconds).
   *                  Used to detect stale transactions.
   * @param confirmedAt Timestamp when transaction was confirmed (Unix milliseconds).
   *                    Available once state reaches Confirmed.
   * @param attempts Number of submission attempts (for retry tracking).
   *                 Increments on each retry after failure. Useful for monitoring.
   */
  case class TransactionStatus(
    requestId: String,
    agentId: String,
    action: String,
    chain: String,
    state: TransactionState,
    txHash: Option[String] = None,
    blockNumber: Option[Long] = None,
    gasUsed: Option[String] = None,
    gasPrice: Option[String] = None,
    error: Option[String] = None,
    createdAt: Long,
    updatedAt: Long,
    confirmedAt: Option[Long] = None,
    attempts: Int = 0
  )

  object TransactionStatus {
    implicit val decoder: JsonDecoder[TransactionStatus] =
      DeriveJsonDecoder.gen[TransactionStatus]
    implicit val encoder: JsonEncoder[TransactionStatus] =
      DeriveJsonEncoder.gen[TransactionStatus]
  }

  /**
   * Response returned immediately after submitting a transaction.
   * 
   * Provides quick feedback to the caller before full transaction processing.
   * The requestId can be used to poll for status updates asynchronously.
   * 
   * @param requestId Unique identifier to track this transaction.
   *                  Use with getTransactionStatus to check progress.
   * @param status Initial status (typically "queued" or "validating").
   * @param message Human-readable message about the submission.
   *                Examples: "Transaction queued successfully",
   *                         "Transaction validated and submitted"
   * @param estimatedGas Estimated gas cost for the transaction (if available).
   *                     Helps users understand expected costs before confirmation.
   *                     May be unavailable if estimation fails.
   */
  case class TransactionSubmitResponse(
    requestId: String,
    status: String,
    message: String,
    estimatedGas: Option[String] = None
  )

  object TransactionSubmitResponse {
    implicit val decoder: JsonDecoder[TransactionSubmitResponse] =
      DeriveJsonDecoder.gen[TransactionSubmitResponse]
    implicit val encoder: JsonEncoder[TransactionSubmitResponse] =
      DeriveJsonEncoder.gen[TransactionSubmitResponse]
  }

  // ==================== Error Models ====================

  /**
   * Base trait for blockchain-specific errors.
   * 
   * Provides typed error handling for blockchain operations, allowing
   * different error types to be handled appropriately at each layer.
   * 
   * All blockchain errors extend Throwable so they can be used with ZIO's
   * error channel and standard exception handling mechanisms.
   */
  sealed trait BlockchainError extends Throwable
  
  object BlockchainError {
    /**
     * Error during transaction validation phase.
     * 
     * Occurs when:
     * - Invalid addresses or parameters
     * - Business rule violations
     * - Insufficient balance checks fail
     * - Smart contract validation fails
     * 
     * These errors occur before blockchain submission.
     * 
     * @param message Detailed explanation of validation failure
     */
    case class ValidationError(message: String) extends BlockchainError
    
    /**
     * Error during transaction execution on the blockchain.
     * 
     * Occurs when:
     * - Smart contract reverts
     * - Out of gas errors
     * - Transaction reverts for business logic reasons
     * - Slippage tolerance exceeded (for swaps)
     * 
     * These errors occur after submission to blockchain.
     * 
     * @param message Detailed explanation of execution failure
     */
    case class ExecutionError(message: String) extends BlockchainError
    
    /**
     * Error indicating insufficient funds for the operation.
     * 
     * Occurs when:
     * - Wallet balance too low for transaction
     * - Insufficient gas token for fees
     * - Token balance insufficient for swap/transfer
     * 
     * Should trigger user notification to add funds.
     * 
     * @param message Detailed explanation of which balance is insufficient
     */
    case class InsufficientFunds(message: String) extends BlockchainError
    
    /**
     * Network-level errors communicating with blockchain.
     * 
     * Occurs when:
     * - RPC endpoint unavailable or timeout
     * - Network congestion prevents submission
     * - Chain reorganization detected
     * - Connection lost to blockchain node
     * 
     * These are often transient and may succeed on retry.
     * 
     * @param message Detailed explanation of network issue
     */
    case class NetworkError(message: String) extends BlockchainError
    
    /**
     * Error indicating requested resource was not found.
     * 
     * Occurs when:
     * - Transaction ID doesn't exist
     * - Querying invalid addresses or contracts
     * - Blockchain data not yet indexed
     * 
     * @param message Detailed explanation of what was not found
     */
    case class NotFoundError(message: String) extends BlockchainError
  }
}
