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

package connectors.exchange

import forms.AssistanceDetailsForm
import play.api.libs.json.Json


case class AssistanceDetailsExchange(hasDisability: String,
                                     hasDisabilityDescription: Option[String],
                                     guaranteedInterview: Option[Boolean],
                                     needsSupportForOnlineAssessment: Boolean,
                                     needsSupportForOnlineAssessmentDescription: Option[String],
                                     needsSupportAtVenue: Boolean,
                                     needsSupportAtVenueDescription: Option[String])

object AssistanceDetailsExchange {
  implicit val assistanceDetailsExchangeFormat = Json.format[AssistanceDetailsExchange]

  implicit class assistanceDetailsFormtoRequest(data: AssistanceDetailsForm.Data) {
    def exchange = AssistanceDetailsExchange(
      data.hasDisability,
      data.hasDisabilityDescription,
      data.guaranteedInterview.map {
        case "Yes" => true
        case "No" => false
        case _ => false},
      data.needsSupportForOnlineAssessment match {
        case "Yes" => true
        case "No" => false
        case _ => false
      },
      data.needsSupportForOnlineAssessmentDescription,
      data.needsSupportAtVenue match {
        case "Yes" => true
        case "No" => false
        case _ => false
      },
      data.needsSupportAtVenueDescription
    )
  }
}
