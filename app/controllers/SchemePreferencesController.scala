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

import _root_.forms.SelectedSchemesForm._
import connectors.SchemeClient.{CannotUpdateSchemePreferences, SchemePreferencesNotFound}
import connectors.{ApplicationClient, SchemeClient}
import helpers.NotificationType._
import security.Roles.SchemesRole

import scala.concurrent.Future

class SchemePreferencesController(applicationClient: ApplicationClient, schemeClient: SchemeClient) extends
  BaseController(applicationClient){

  def present = CSRSecureAppAction(SchemesRole) { implicit request =>
    implicit user =>
      schemeClient.getSchemePreferences(user.application.applicationId).map { selectedSchemes =>
        Ok(views.html.application.schemePreferences.schemeSelection(form.fill(selectedSchemes)))
      }.recover {
        case e: SchemePreferencesNotFound =>
          Ok(views.html.application.schemePreferences.schemeSelection(form))
      }
  }

  def submit = CSRSecureAppAction(SchemesRole) { implicit request =>
    implicit user =>
      form.bindFromRequest.fold(
        invalidForm => {
          Future.successful(Ok(views.html.application.schemePreferences.schemeSelection(invalidForm)))
        },
        selectedSchemes => {
          (for {
            _ <- schemeClient.updateSchemePreferences(selectedSchemes)(user.application.applicationId)
            redirect <- refreshCachedUser().map(_ => Redirect(routes.AssistanceDetailsController.present()))
          } yield {
            redirect
          })
        }
    )
  }
}

object SchemePreferencesController extends SchemePreferencesController(ApplicationClient, SchemeClient)
