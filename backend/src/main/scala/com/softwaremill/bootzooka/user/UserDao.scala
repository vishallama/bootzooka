package com.softwaremill.bootzooka.user

import java.util.UUID

import com.softwaremill.bootzooka.common.FutureHelpers._
import com.softwaremill.bootzooka.sql.SqlDatabase
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

class UserDao(protected val database: SqlDatabase)(implicit val ec: ExecutionContext) extends SqlUserSchema {

  import database._
  import database.driver.api._

  type UserId = UUID

  def add(user: User): Future[Unit] = {
    val action = (for {
      userByLoginOpt <- findByLowerCasedLoginAction(user.login)
      userByEmailOpt <- findByEmailAction(user.email)
      _ <- addOrFailOnExistingAction(userByLoginOpt, userByEmailOpt, user)
    } yield ()).transactionally

    db.run(action)
  }

  private def addOrFailOnExistingAction(userByLoginOpt: Option[User], userByEmailOpt: Option[User], user: User) = {
    if (userByLoginOpt.isDefined || userByEmailOpt.isDefined)
      DBIO.failed(new IllegalArgumentException("User with given e-mail or login already exists"))
    else
      users += user
  }

  def findById(userId: UserId): Future[Option[User]] =
    findOneWhere(_.id === userId)

  private def findOneWhereAction(condition: Users => Rep[Boolean]) = {
    users.filter(condition).result.headOption
  }

  private def findByEmailAction(email: String) = findOneWhereAction(_.email.toLowerCase === email.toLowerCase)

  private def findByLowerCasedLoginAction(login: String) = findOneWhereAction(_.loginLowerCase === login.toLowerCase)

  private def findOneWhere(condition: Users => Rep[Boolean]): Future[Option[User]] = {
    db.run(findOneWhereAction(condition))
  }

  def findByEmail(email: String) = db.run(findByEmailAction(email))

  def findByLowerCasedLogin(login: String) = db.run(findByLowerCasedLoginAction(login))

  def findByLoginOrEmail(loginOrEmail: String) = {
    findByLowerCasedLogin(loginOrEmail).flatMap(userOpt =>
      userOpt.map(user => Future { Some(user) }).getOrElse(findByEmail(loginOrEmail)))
  }

  def changePassword(userId: UserId, newPassword: String): Future[Unit] = {
    db.run(users.filter(_.id === userId).map(_.password).update(newPassword)).mapToUnit
  }

  def changeLogin(userId: UserId, newLogin: String): Future[Unit] = {
    val action = users.filter(_.id === userId).map { user =>
      (user.login, user.loginLowerCase)
    }.update((newLogin, newLogin.toLowerCase))
    db.run(action).mapToUnit
  }

  def changeEmail(userId: UserId, newEmail: String): Future[Unit] = {
    db.run(users.filter(_.id === userId).map(_.email).update(newEmail)).mapToUnit
  }
}

/**
 * The schemas are in separate traits, so that if your DAO would require to access (e.g. join) multiple tables,
 * you can just mix in the necessary traits and have the `TableQuery` definitions available.
 */
trait SqlUserSchema {

  protected val database: SqlDatabase

  import database._
  import database.driver.api._

  protected val users = TableQuery[Users]

  protected class Users(tag: Tag) extends Table[User](tag, "users") {
    // format: OFF
    def id              = column[UUID]("id", O.PrimaryKey)
    def login           = column[String]("login")
    def loginLowerCase  = column[String]("login_lowercase")
    def email           = column[String]("email")
    def password        = column[String]("password")
    def salt            = column[String]("salt")
    def createdOn       = column[DateTime]("created_on")

    def * = (id, login, loginLowerCase, email, password, salt, createdOn) <> ((User.apply _).tupled, User.unapply)
    // format: ON
  }

}