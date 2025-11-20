package viewmodels.checkAnswers

import controllers.routes
import uk.gov.hmrc.securitiestransferchargeregistration.models.{CheckMode, UserAnswers}
import pages.$className$Page
import play.api.i18n.{Lang, Messages}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.securitiestransferchargeregistration.utils.DateTimeFormats.dateTimeFormat
import uk.gov.hmrc.securitiestransferchargeregistration.viewmodels.govuk.summarylist._
import uk.gov.hmrc.securitiestransferchargeregistration.viewmodels.implicits._

object $className$Summary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get($className$Page).map {
      answer =>

        implicit val lang: Lang = messages.lang

        SummaryListRowViewModel(
          key     = "$className;format="decap"$.checkYourAnswersLabel",
          value   = ValueViewModel(answer.format(dateTimeFormat())),
          actions = Seq(
            ActionItemViewModel("site.change", routes.$className$Controller.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("$className;format="decap"$.change.hidden"))
          )
        )
    }
}
