package controllers.actions

import base.*
import base.Fixtures.{FakeAuthConnectorFailing, FakeAuthConnectorSuccess}
import org.scalatest.RecoverMethods.recoverToExceptionIf
import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents, Results}
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.{ItmpName, ~}
import uk.gov.hmrc.http.UnauthorizedException
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.AuthenticatedStcAction
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.StcAuthRequest

import scala.concurrent.{ExecutionContext, Future}

class StcAuthActionSpec extends SpecBase {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def buildRetrieval(internalId: String,
                     enrolments: Enrolments,
                     affinityGroup: AffinityGroup,
                     confidenceLevel: ConfidenceLevel,
                     nino: Option[String],
                     itmpName: Option[ItmpName]) =
    new ~(
      new ~(
        new ~(
          new ~(
            new ~(Some(internalId), enrolments),
            Some(affinityGroup)
          ),
          confidenceLevel
        ),
        nino
      ),
      itmpName
    )

  "AuthenticatedStcAction" - {

    "must build a StcAuthRequest and invoke the block when authorised" in {
      val application = applicationBuilder().build()

      running(application) {
        val mcc = application.injector.instanceOf[MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        // construct a successful retrieval tuple
        val retrievalValue = buildRetrieval(
          "internal-1",
          Enrolments(Set()),
          AffinityGroup.Individual,
          ConfidenceLevel.L250,
          Some("AA123456A"),
          Some(ItmpName(Some("First"), None, Some("Last")))
        )

        val authConnector = new FakeAuthConnectorSuccess(retrievalValue)

        val bodyParsers = application.injector.instanceOf[play.api.mvc.BodyParsers.Default]
        val action = new AuthenticatedStcAction(authConnector, appConfig, bodyParsers)

        var captured: Option[StcAuthRequest[AnyContentAsEmpty.type]] = None

        val result = action.invokeBlock(FakeRequest(), { stc =>
          captured = Some(stc.asInstanceOf[StcAuthRequest[AnyContentAsEmpty.type]])
          Future.successful(Results.Ok)
        })

        status(result) mustBe OK
        captured mustBe defined
        val stc = captured.get
        stc.userId mustBe "internal-1"
        stc.enrolments mustBe Enrolments(Set())
        stc.affinityGroup mustBe AffinityGroup.Individual
        stc.confidenceLevel mustBe ConfidenceLevel.L250
        stc.maybeNino mustBe Some("AA123456A")
      }
    }

    "must redirect to the login page when there is no active session" in {
      val application = applicationBuilder().build()

      running(application) {
        val mcc = application.injector.instanceOf[MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        val authConnector = new FakeAuthConnectorFailing(new NoActiveSession("no-session") {})

        val bodyParsers = application.injector.instanceOf[play.api.mvc.BodyParsers.Default]
        val action = new AuthenticatedStcAction(authConnector, appConfig, bodyParsers)

        val result = action.invokeBlock(FakeRequest(), { _ => Future.successful(Results.Ok) })

        status(result) mustBe SEE_OTHER
        // Redirect should contain the login URL and continue parameter
        redirectLocation(result).get must include(appConfig.loginUrl)
      }
    }

    "must redirect to the unauthorised page when authorisation fails" in {
      val application = applicationBuilder().build()

      running(application) {
        val mcc = application.injector.instanceOf[MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        val authConnector = new FakeAuthConnectorFailing(new AuthorisationException("fail") {})

        val bodyParsers = application.injector.instanceOf[play.api.mvc.BodyParsers.Default]
        val action = new AuthenticatedStcAction(authConnector, appConfig, bodyParsers)

        val result = action.invokeBlock(FakeRequest(), { _ => Future.successful(Results.Ok) })

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
      }
    }

    "must fail with UnauthorizedException when internalId or affinityGroup are missing in the retrieval" in {
     val application = applicationBuilder().build()

     running(application) {
       val appConfig = application.injector.instanceOf[FrontendAppConfig]

       // Build a retrieval tuple with None for internalId and/or affinityGroup
       val retrievalMissingInternal = new ~(
         new ~(
           new ~(
             new ~(
               new ~(None: Option[String], Enrolments(Set())),
               Some(AffinityGroup.Individual)
             ),
             ConfidenceLevel.L250
           ),
           Some("AA123456A")
         ),
         Some(ItmpName(Some("First"), None, Some("Last")))
       )

       val authConnector = new FakeAuthConnectorSuccess(retrievalMissingInternal)

       val bodyParsers = application.injector.instanceOf[play.api.mvc.BodyParsers.Default]
       val action = new AuthenticatedStcAction(authConnector, appConfig, bodyParsers)

       // Expect the future to fail with UnauthorizedException because pattern match falls through to case _
       val thrown = recoverToExceptionIf[UnauthorizedException] {
         action.invokeBlock(FakeRequest(), _ => Future.successful(Results.Ok))
       }

       whenReady(thrown) { ex =>
         ex.getMessage must include("Unable to retrieve internalId or affinityGroup from auth")
       }
     }
   }

   }
 }

