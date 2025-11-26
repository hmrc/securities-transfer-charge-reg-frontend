package controllers

import base.SpecBase
import play.api.mvc.{AnyContent, AnyContentAsEmpty}
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.retrieve.{ItmpName, Retrieval, ~}
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, ConfidenceLevel, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.{EnrolmentCheck, IdentifierAction}
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.{Redirects, RegistrationController}

import scala.concurrent.{ExecutionContext, Future}

class RegistrationControllerSpec extends SpecBase {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  // simple stub AuthConnector that returns a preconfigured value for any retrieval
  class FakeAuthConnector[T](value: T) extends AuthConnector {
    val serviceUrl: String = ""
    override def authorise[A](predicate: uk.gov.hmrc.auth.core.authorise.Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      Future.successful(value.asInstanceOf[A])
  }

  // Simple pass-through IdentifierAction used instead of the real AuthenticatedIdentifierAction
  class TestIdentifierAction extends IdentifierAction {
    override def invokeBlock[A](request: play.api.mvc.Request[A], block: uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.IdentifierRequest[A] => Future[play.api.mvc.Result]): Future[play.api.mvc.Result] =
      block(uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.IdentifierRequest(request, "test-internal-id"))

    override def parser: play.api.mvc.BodyParser[AnyContent] = Helpers.stubBodyParser(AnyContentAsEmpty)

    override protected def executionContext: ExecutionContext = ec
  }

  // Pass-through EnrolmentCheck that just invokes the block
  class PassThroughEnrolmentCheck extends EnrolmentCheck {
    override def parser: play.api.mvc.BodyParser[AnyContent] = Helpers.stubBodyParser(AnyContentAsEmpty)
    override protected def executionContext: ExecutionContext = ec
    override def invokeBlock[A](request: play.api.mvc.Request[A], block: play.api.mvc.Request[A] => Future[play.api.mvc.Result]): Future[play.api.mvc.Result] = block(request)
  }

  def buildRetrieval(affinityGroup: AffinityGroup, confidenceLevel: ConfidenceLevel, nino: Option[String], itmpName: Option[ItmpName])
    : Option[AffinityGroup] ~ ConfidenceLevel ~ Option[String] ~ Option[ItmpName] = {
    new ~(
      new ~(
        new ~(Some(affinityGroup), confidenceLevel), nino), itmpName)
  }

  "RegistrationController" - {

    "should redirect organisation users to organisation registration" in {
      val application = applicationBuilder().configure().build()

      running(application) {
        val mcc = application.injector.instanceOf[play.api.mvc.MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects = application.injector.instanceOf[Redirects]

        val retrievalValue = buildRetrieval(Organisation, ConfidenceLevel.L50, None, None)

        val authConnectorForController = new FakeAuthConnector(retrievalValue)

        //val identifierAction = new TestIdentifierAction
        val enrolmentCheck = new PassThroughEnrolmentCheck

        val controller = new RegistrationController(mcc, appConfig, redirects, authConnectorForController, enrolmentCheck)

        val result = controller.routingLogic.apply(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.registerOrganisationUrl)
      }
    }

    "should redirect agents to ASA" in {
      val application = applicationBuilder().configure().build()

      running(application) {
        val mcc = application.injector.instanceOf[play.api.mvc.MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects = application.injector.instanceOf[Redirects]

        val retrievalValue = buildRetrieval(Agent, ConfidenceLevel.L50, None, None)
        val authConnectorForController = new FakeAuthConnector(retrievalValue)

        val enrolmentCheck = new PassThroughEnrolmentCheck

        val controller = new RegistrationController(mcc, appConfig, redirects, authConnectorForController, enrolmentCheck)

        val result = controller.routingLogic.apply(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.asaUrl)
      }
    }

    "should redirect individuals with insufficient confidence to IV uplift" in {
      val application = applicationBuilder().configure().build()

      running(application) {
        val mcc = application.injector.instanceOf[play.api.mvc.MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects = application.injector.instanceOf[Redirects]

        val retrievalValue = buildRetrieval(Individual, ConfidenceLevel.L50, None, None)
        val authConnectorForController = new FakeAuthConnector(retrievalValue)

        val enrolmentCheck = new PassThroughEnrolmentCheck

        val controller = new RegistrationController(mcc, appConfig, redirects, authConnectorForController, enrolmentCheck)

        val result = controller.routingLogic.apply(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.ivUpliftUrl)
      }
    }

    "should redirect individuals with sufficient confidence but no NINO IV uplift" in {
      val application = applicationBuilder().configure().build()

      running(application) {
        val mcc = application.injector.instanceOf[play.api.mvc.MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects = application.injector.instanceOf[Redirects]

        val retrievalValue = buildRetrieval(Individual, ConfidenceLevel.L250, None, Some(ItmpName(Some("First"), None, Some("Last"))))
        val authConnectorForController = new FakeAuthConnector(retrievalValue)

        val enrolmentCheck = new PassThroughEnrolmentCheck

        val controller = new RegistrationController(mcc, appConfig, redirects, authConnectorForController, enrolmentCheck)

        val result = controller.routingLogic.apply(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.ivUpliftUrl)
      }
    }

    "should redirect individuals with sufficient confidence but no forename to IV Uplift" in {
      val application = applicationBuilder().configure().build()

      running(application) {
        val mcc = application.injector.instanceOf[play.api.mvc.MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects = application.injector.instanceOf[Redirects]

        val retrievalValue = buildRetrieval(Individual, ConfidenceLevel.L250, Some("NZ153756A"), Some(ItmpName(None, None, Some("Last"))))
        val authConnectorForController = new FakeAuthConnector(retrievalValue)

        val enrolmentCheck = new PassThroughEnrolmentCheck

        val controller = new RegistrationController(mcc, appConfig, redirects, authConnectorForController, enrolmentCheck)

        val result = controller.routingLogic.apply(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.ivUpliftUrl)
      }
    }

    "should redirect individuals with sufficient confidence and other data to individual registration" in {
      val application = applicationBuilder().configure().build()

      running(application) {
        val mcc = application.injector.instanceOf[play.api.mvc.MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects = application.injector.instanceOf[Redirects]

        val retrievalValue = buildRetrieval(Individual, ConfidenceLevel.L250, Some("NZ153756A"), Some(ItmpName(Some("First"), None, Some("Last"))))
        val authConnectorForController = new FakeAuthConnector(retrievalValue)

        val enrolmentCheck = new PassThroughEnrolmentCheck

        val controller = new RegistrationController(mcc, appConfig, redirects, authConnectorForController, enrolmentCheck)

        val result = controller.routingLogic.apply(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.registerIndividualUrl)
      }
    }
  }
}

