/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import _root_.forms.AssistanceDetailsFormExamples
import com.github.tomakehurst.wiremock.client.WireMock.{any => _}
import config.CSRHttp
import connectors.ApplicationClient
import connectors.ApplicationClient.AssistanceDetailsNotFound
import connectors.exchange.ProgressResponseExamples
import models.ApplicationData.ApplicationStatus
import models.SecurityUserExamples._
import models.{AssistanceDetailsExchangeExamples, CachedDataExample, CachedDataWithApp, SecurityUserExamples}
import models.services.UserService
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class AssistanceDetailsControllerSpec extends BaseControllerSpec {

   // This is the implicit user
   override def currentCandidateWithApp: CachedDataWithApp = CachedDataWithApp(ActiveCandidate.user,
    CachedDataExample.InFrameworkDetailsApplication.copy(userId = ActiveCandidate.user.userID))

  "present" should {
    "load assistance details page for the new user" in new TestFixture {
      when(mockApplicationClient.findAssistanceDetails(eqTo(currentUserId), eqTo(currentApplicationId))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new AssistanceDetailsNotFound))

      val result = controller.present()(fakeRequest)
      status(result) must be(OK)
      val content = contentAsString(result)
      content must include("<title>Disability and health conditions")
      content must include(s"""<span class="your-name" id="bannerUserName">${currentCandidate.user.preferredName.get}</span>""")
    }

    "load assistance details page for the already created assistance details" in new TestFixture {
      when(mockApplicationClient.findAssistanceDetails(eqTo(currentUserId), eqTo(currentApplicationId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(AssistanceDetailsExchangeExamples.DisabilityGisAndAdjustments ))

      val result = controller.present()(fakeRequest)

      status(result) must be(OK)
      val content = contentAsString(result)
      content must include("<title>Disability and health conditions")
      content must include(s"""<span class="your-name" id="bannerUserName">${currentCandidate.user.preferredName.get}</span>""")
      content must include("online adjustment description xxx")
    }
  }

  "submit assistance details" should {
    "update candidate's details" in new TestFixture {
      val Request = fakeRequest.withFormUrlEncodedBody(AssistanceDetailsFormExamples.DisabilityGisAndAdjustmentsFormUrlEncodedBody: _*)
      when(mockApplicationClient.updateAssistanceDetails(eqTo(currentApplicationId), eqTo(currentUserId),
        eqTo(AssistanceDetailsFormExamples.DisabilityGisAndAdjustmentsForm))(any[HeaderCarrier])).thenReturn(Future.successful(()))

      when(mockApplicationClient.getApplicationProgress(eqTo(currentApplicationId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(ProgressResponseExamples.InAssistanceDetails))
      val Application = currentCandidateWithApp.application
        .copy(progress = ProgressResponseExamples.InAssistanceDetails, applicationStatus = ApplicationStatus.IN_PROGRESS)
      val UpdatedCandidate = currentCandidate.copy(application = Some(Application))
      when(mockUserService.save(eqTo(UpdatedCandidate))(any[HeaderCarrier])).thenReturn(Future.successful(UpdatedCandidate))

      val result = controller.submit()(Request)

      status(result) must be(SEE_OTHER)
//      redirectLocation(result) must be(Some(routes.ReviewApplicationController.present().url))
      redirectLocation(result) must be(Some(routes.QuestionnaireController.start().url))
    }

    "fail updating the candidate when person cannot be found" in new TestFixture {
      val Request = fakeRequest.withFormUrlEncodedBody(AssistanceDetailsFormExamples.DisabilityGisAndAdjustmentsFormUrlEncodedBody: _*)
      when(mockApplicationClient.updateAssistanceDetails(eqTo(currentApplicationId), eqTo(currentUserId),
        eqTo(AssistanceDetailsFormExamples.DisabilityGisAndAdjustmentsForm))(any[HeaderCarrier])).
        thenReturn(Future.failed(new AssistanceDetailsNotFound))

      val result = controller.submit()(Request)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.HomeController.present().url))
      flash(result).data must be (Map("danger" -> "account.error"))
    }
  }

  trait TestFixture {
    val mockApplicationClient = mock[ApplicationClient]
    val mockSecurityEnvironment = mock[security.SecurityEnvironment]
    val mockUserService = mock[UserService]

    class TestableAssistanceDetailsController extends AssistanceDetailsController(mockApplicationClient)
      with TestableSecureActions {
      val http: CSRHttp = CSRHttp
      override protected def env = mockSecurityEnvironment
      when(mockSecurityEnvironment.userService).thenReturn(mockUserService)
    }

    def controller = new TestableAssistanceDetailsController

  }
}
