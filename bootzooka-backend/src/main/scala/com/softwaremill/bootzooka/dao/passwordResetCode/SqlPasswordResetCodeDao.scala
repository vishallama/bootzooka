package com.softwaremill.bootzooka.dao.passwordResetCode

import com.softwaremill.bootzooka.dao.sql.SqlDatabase
import com.softwaremill.bootzooka.domain.PasswordResetCode

import scala.language.implicitConversions

class SqlPasswordResetCodeDao(protected val database: SqlDatabase) extends PasswordResetCodeDao with SqlPasswordResetCodeSchema {

  import database._
  import database.driver.simple._

  override def store(code: PasswordResetCode): Unit = db.withSession { implicit session =>
    passwordResetCodes.insert(code)
  }

  override def load(code: String): Option[PasswordResetCode] = db.withSession { implicit session =>
    passwordResetCodes.filter(_.code === code).firstOption
  }

  override def delete(code: PasswordResetCode): Unit = db.withTransaction { implicit session =>
    passwordResetCodes.filter(_.id === code.id).delete
  }
}