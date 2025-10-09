package com.scaliaos.app

import sttp.tapir.server.ziohttp._
import zio._
import zio.http.Server
import com.scaliaos.app.http.endpoints.AgentExecutionEndpoint  
import com.scaliaos.app.services.{BlockchainService, AgentRegistry, AgentExecutorFactory}

/**
 * Main entry point for the ScaliaOS v0.2 server.
 * 
 * ScaliaOS is an agent execution platform that provides a registry-based architecture
 * for managing and executing various types of agents (LLM, Blockchain, and Hybrid).
 * 
 * The server exposes HTTP endpoints for:
 * - Executing agents
 * - Listing registered agents
 * - Retrieving agent status/configuration
 */
object Main extends ZIOAppDefault {

  /**
   * Core server program that wires together all services and starts the HTTP server.
   * 
   * This uses ZIO's dependency injection to access:
   * - BlockchainService: Handles blockchain interactions (currently mocked)
   * - AgentRegistry: Maintains catalog of available agents
   * - AgentExecutorFactory: Creates executor instances for running agents
   * 
   * The program constructs HTTP routes from Tapir endpoint definitions and serves
   * them via ZIO HTTP.
   */
  val serverProgram = 
    ZIO.serviceWithZIO[BlockchainService] { blockchainService =>
      ZIO.serviceWithZIO[AgentRegistry] { registry =>
        ZIO.serviceWithZIO[AgentExecutorFactory] { factory =>
          
          // Create v0.2 endpoints with registry-based routing
          val allEndpoints = AgentExecutionEndpoint.allEndpoints(
            blockchainService, 
            registry, 
            factory
          )
          
          // Convert Tapir endpoint definitions to ZIO HTTP routes
          val routes = ZioHttpInterpreter(ZioHttpServerOptions.default).toHttp(allEndpoints)
          
          // Start serving HTTP requests
          Server.serve(routes)
        }
      }
    }

  /**
   * Application entry point.
   * 
   * Prints the ASCII art banner with server information, then starts the server
   * with all required dependencies provided through the ZIO environment.
   */
  override def run =
    for {
      // Display startup banner with server configuration and available endpoints
      _ <- Console.printLine(
        """
        |-------------------------------------------------------------------------------------------------------------------------------------------------
        |          
        |
        |     SSSSSSSSSSSSSSS                                       lllllll   iiii                        OOOOOOOOO        SSSSSSSSSSSSSSS 
        |   SS:::::::::::::::S                                      l:::::l  i::::i                     OO:::::::::OO    SS:::::::::::::::S
        |  S:::::SSSSSS::::::S                                      l:::::l   iiii                    OO:::::::::::::OO S:::::SSSSSS::::::S
        |  S:::::S     SSSSSSS                                      l:::::l                          O:::::::OOO:::::::OS:::::S     SSSSSSS
        |  S:::::S                cccccccccccccccc  aaaaaaaaaaaaa    l::::l iiiiiii   aaaaaaaaaaaaa  O::::::O   O::::::OS:::::S            
        |  S:::::S              cc:::::::::::::::c  a::::::::::::a   l::::l i:::::i   a::::::::::::a O:::::O     O:::::OS:::::S            
        |   S::::SSSS          c:::::::::::::::::c  aaaaaaaaa:::::a  l::::l  i::::i   aaaaaaaaa:::::aO:::::O     O:::::O S::::SSSS         
        |    SS::::::SSSSS    c:::::::cccccc:::::c           a::::a  l::::l  i::::i            a::::aO:::::O     O:::::O  SS::::::SSSSS    
        |      SSS::::::::SS  c::::::c     ccccccc    aaaaaaa:::::a  l::::l  i::::i     aaaaaaa:::::aO:::::O     O:::::O    SSS::::::::SS  
        |         SSSSSS::::S c:::::c               aa::::::::::::a  l::::l  i::::i   aa::::::::::::aO:::::O     O:::::O       SSSSSS::::S 
        |              S:::::Sc:::::c              a::::aaaa::::::a  l::::l  i::::i  a::::aaaa::::::aO:::::O     O:::::O            S:::::S
        |              S:::::Sc::::::c     ccccccca::::a    a:::::a  l::::l  i::::i a::::a    a:::::aO::::::O   O::::::O            S:::::S
        |  SSSSSSS     S:::::Sc:::::::cccccc:::::ca::::a    a:::::a l::::::li::::::ia::::a    a:::::aO:::::::OOO:::::::OSSSSSSS     S:::::S
        |  S::::::SSSSSS:::::S c:::::::::::::::::ca:::::aaaa::::::a l::::::li::::::ia:::::aaaa::::::a OO:::::::::::::OO S::::::SSSSSS:::::S
        |  S:::::::::::::::SS   cc:::::::::::::::c a::::::::::aa:::al::::::li::::::i a::::::::::aa:::a  OO:::::::::OO   S:::::::::::::::SS 
        |   SSSSSSSSSSSSSSS       cccccccccccccccc  aaaaaaaaaa  aaaalllllllliiiiiiii  aaaaaaaaaa  aaaa    OOOOOOOOO      SSSSSSSSSSSSSSS         
        |
        |
        |-------------------------------------------------------------------------------------------------------------------------------------------------
        |                 Scalia v0.2 Server (Registry-Based Architecture)
        |-------------------------------------------------------------------------------------------------------------------------------------------------
        |  Host: localhost
        |  Port: 8000
        |  
        |  Endpoints:
        |    POST http://localhost:8000/agent/run        - Execute an agent
        |    GET  http://localhost:8000/agent/list       - List all registered agents
        |    GET  http://localhost:8000/agent/:id/status - Get agent configuration
        |  
        |  Registered Agents:
        |    - llm-chat-gpt4   (LLM)(Live Need API Key)  - General purpose chat
        |    - solana-trading  (Blockchain)(Mock)        - Trading on Solana
        |    - ai-trader       (Hybrid)(Mock)            - AI-powered trading
        |  
        |  Documentation:
        |    https://github.com/orbit-sca/ScaliaOS
        |-------------------------------------------------------------------------------------------------------------------------------------------------
        """.stripMargin
      )
      
      // Start the server with dependency injection
      // - Server runs on port 8000
      // - BlockchainService is mocked for development
      // - AgentRegistry and AgentExecutorFactory use live implementations
      _ <- serverProgram.provide(
        Server.defaultWithPort(8000),
        BlockchainService.mock,
        AgentRegistry.live,
        AgentExecutorFactory.live
      )
    } yield ()
}