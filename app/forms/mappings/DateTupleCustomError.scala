/*
 * Copyright 2023 HM Revenue & Customs
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

package forms.mappings

import org.joda.time.LocalDate
import play.api.data.Forms.{optional, text, tuple}
import play.api.data.{FormError, Mapping}

import java.time.YearMonth

case class DateTupleCustomError(invalidDateErrorKey: String){

  val dateTuple: Mapping[Option[LocalDate]] = dateTuple()

  def mandatoryDateTuple(error: String): Mapping[LocalDate] =
    dateTuple.verifying(error, data => data.isDefined).transform(o => o.get, v => if (v == null) None else Some(v))

  //scalastyle:off cyclomatic.complexity
  private def dateTuple(validate: Boolean = true): Mapping[Option[LocalDate]] =
    tuple(
      "year"  -> optional(text),
      "month" -> optional(text),
      "day"   -> optional(text)
    ).verifying(
      invalidDateErrorKey,
      data =>
        (data._1, data._2, data._3) match {
          case (None, None, None)                   => true
          case (yearOption, monthOption, dayOption) =>
            try {
              val y = yearOption.getOrElse(throw new Exception("Year missing")).trim
              if (y.length != 4) {
                throw new Exception("Year must be 4 digits")
              }
              new LocalDate(
                y.toInt,
                monthOption.getOrElse(throw new Exception("Month missing")).trim.toInt,
                dayOption.getOrElse(throw new Exception("Day missing")).trim.toInt
              )
              true
            } catch {
              case _: Throwable =>
                if (validate) {
                  false
                } else {
                  true
                }
            }
        }).transform(
      {
        case (Some(y), Some(m), Some(d)) =>
          try Some(new LocalDate(y.trim.toInt, m.trim.toInt, d.trim.toInt))
          catch {
            case e: Exception => None
          }
        case (a, b, c)                   => None
      },
      (date: Option[LocalDate]) =>
        date match {
          case Some(d) => (Some(d.getYear.toString), Some(d.getMonthOfYear.toString), Some(d.getDayOfMonth.toString))
          case _       => (None, None, None)
        }
    )

   def dateTupleOptional(): Mapping[Option[LocalDate]] =
    tuple(
      "year" -> optional(text),
      "month" -> optional(text),
      "day" -> optional(text)
    ).transform(
      {
        case (Some(y), Some(m), Some(d)) =>
          try Some(new LocalDate(y.trim.toInt, m.trim.toInt, d.trim.toInt))
          catch {
            case e: Exception => None
          }
        case (a, b, c) => None
      },
      (date: Option[LocalDate]) =>
        date match {
          case Some(d) => (Some(d.getYear.toString), Some(d.getMonthOfYear.toString), Some(d.getDayOfMonth.toString))
          case _ => (None, None, None)
        }
    )

}

case object DateTupleCustomError {

  def validateDateFields(day: Option[String], month: Option[String], year: Option[String], dateFields: Seq[(String, String)], dateForPastValidation: Option[LocalDate] = None): Seq[FormError] = {
    dateFields.flatMap { x =>
      ((day, month, year): @unchecked) match {
        case (None, None, None) => Seq(FormError(s"${x._1}", s"ated.error.date.empty", Seq(x._2)))
        case (Some(d), Some(m), Some(y)) =>
          if (d.isEmpty && m.isEmpty && y.isEmpty) {
            Seq(FormError(s"${x._1}", s"ated.error.date.empty", Seq(x._2)))
          } else if (d.isEmpty && m.nonEmpty && y.nonEmpty) {
            Seq(FormError(s"${x._1}.day", s"ated.error.date.day.missing", Seq(x._2)))
          } else if (!d.isEmpty && m.isEmpty && y.nonEmpty) {
            Seq(FormError(s"${x._1}.month", s"ated.error.date.month.missing", Seq(x._2)))
          } else if (d.nonEmpty && m.nonEmpty && y.isEmpty) {
            Seq(FormError(s"${x._1}.year", s"ated.error.date.year.missing", Seq(x._2)))
          } else if (d.isEmpty && m.isEmpty && y.nonEmpty) {
            Seq(FormError(s"${x._1}.day", s"ated.error.date.daymonth.missing", Seq(x._2)))
          } else if (d.isEmpty && m.nonEmpty && y.isEmpty) {
            Seq(FormError(s"${x._1}.day", s"ated.error.date.dayyear.missing", Seq(x._2)))
          } else if (d.nonEmpty && m.isEmpty && y.isEmpty)
            Seq(FormError(s"${x._1}.month", s"ated.error.date.monthyear.missing", Seq(x._2)))
          else {
            try {
              val day = d.trim.toInt
              val month = m.trim.toInt
              val year = y.trim.toInt
              val validLeapYear = 2020

              if (!(day >= 1 && day <= 31)) {
                Seq(FormError(s"${x._1}.day", s"ated.error.day.invalid", Seq(x._2)))
              }
              else if (!(month >= 1 && month <= 12)) {
                Seq(FormError(s"${x._1}.month", s"ated.error.month.invalid", Seq(x._2)))
              }
              else if (y.trim.length != 4) {
                Seq(FormError(s"${x._1}.year", s"ated.error.date.year.length", Seq(x._2)))
              }
              else if (!YearMonth.of(validLeapYear, month).isValidDay(day)) {
                Seq(FormError(s"${x._1}.day", s"ated.error.date.invalid.day.month", Seq(x._2)))
              }
              else {
                val validatedDate = new LocalDate(y.trim.toInt, m.trim.toInt, d.trim.toInt)
                dateForPastValidation match {
                  case Some(pastDate) if(validatedDate.isBefore(pastDate)) =>
                    Seq(FormError(s"${x._1}.day", s"ated.error.date.past", Seq(x._2)))
                  case _ => Seq()
                }
              }
            } catch {
              case _: Throwable => Seq(FormError(s"${x._1}.day", s"ated.error.date.invalid", Seq(x._2)))
            }
          }
      }
    }
  }

}


