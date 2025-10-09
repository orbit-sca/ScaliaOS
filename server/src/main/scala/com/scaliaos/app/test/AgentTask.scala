/*
package com.scaliaos.app.test

import com.scaliaos.app.models.AgentResult
import com.scaliaos.app.models.AgentTask
import zio.*
import sttp.client3.*



object AgentTask extends ZIOAppDefault {
  val backendLayer = AsyncHttpClientZioBackend.layer()
  
  override def run = {
    val task = AgentTask("moderator", "This is a test message")
    
    val request = basicRequest
      .post(uri"http://localhost:8000/run-agent")
      .body(task)
      .response(asJson[AgentResult])
    
    val program = for {
      backend <- ZIO.service[SttpBackend[Task, Any]]
      response <- request.send(backend)
      _ <- ZIO.succeed((println(s"Response: $response")))
    } yield()
    
    program.provideLayer(backendLayer)
  }

}
 
 
 */