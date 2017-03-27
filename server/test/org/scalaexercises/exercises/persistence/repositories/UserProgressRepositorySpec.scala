/*
 * scala-exercises - server
 * Copyright (C) 2015-2016 47 Degrees, LLC. <http://www.47deg.com>
 */

package org.scalaexercises.exercises.persistence.repositories

/*
 * scala-exercises-server
 * Copyright (C) 2015-2016 47 Degrees, LLC. <http://www.47deg.com>
 */

import org.scalaexercises.types.user.UserCreation
import org.scalaexercises.types.progress.SaveUserProgress
import org.scalaexercises.exercises.support.{ArbitraryInstances, DatabaseInstance}
import doobie.imports._
import org.scalacheck.Arbitrary
import org.scalacheck.Shapeless._
import org.scalatest._
import org.scalatest.prop.GeneratorDrivenPropertyChecks

import scalaz.concurrent.Task
import org.scalaexercises.types.user.User
import cats.implicits._
import doobie.util.iolite

import scalaz.syntax.apply._

class UserProgressRepositorySpec
    extends PropSpec
    with GeneratorDrivenPropertyChecks
    with Matchers
    with ArbitraryInstances
    with DatabaseInstance
    with BeforeAndAfterAll {

  implicit val trx: Transactor[iolite.IOLite] = databaseTransactor

  val repository: UserProgressRepository = implicitly[UserProgressRepository]
  val userRepository: UserRepository     = implicitly[UserRepository]

  def assertConnectionIO(cio: ConnectionIO[Boolean]): Assertion =
    assert(cio.transact(trx).unsafePerformIO)

  override def beforeAll(): Unit =
    (repository.deleteAll() *> userRepository.deleteAll()).transact(trx).unsafePerformIO

  def newUser(usr: UserCreation.Request): ConnectionIO[User] =
    for {
      maybeUser ← userRepository.create(usr)
    } yield maybeUser.toOption.get

  ignore("new user progress records can be created") {
    forAll(maxDiscardedFactor(10000d)) {
      (usr: UserCreation.Request, prg: SaveUserProgress.Request) ⇒
        val tx: ConnectionIO[Boolean] = for {
          user ← newUser(usr)
          progress = prg.copy(user = user)
          userProgress ← repository.create(progress)
        } yield userProgress == progress.asUserProgress(userProgress.id)

        assertConnectionIO(tx)
    }
  }

  ignore("existing user progress records can be updated") {
    forAll(maxDiscardedFactor(10000d)) {
      (usr: UserCreation.Request, prg: SaveUserProgress.Request, someArgs: List[String]) ⇒
        val tx: ConnectionIO[Boolean] = for {
          user ← newUser(usr)
          progress = prg.copy(user = user)
          _ ← repository.create(progress)
          updatedProgress = progress.copy(args = someArgs)
          userProgress ← repository.update(updatedProgress)
        } yield userProgress.equals(updatedProgress.asUserProgress(userProgress.id))

        assertConnectionIO(tx)
    }
  }

  ignore("user progress can be fetched by section") {
    forAll(maxDiscardedFactor(10000d)) {
      (usr: UserCreation.Request, prg: SaveUserProgress.Request) ⇒
        val tx: ConnectionIO[Boolean] = for {
          user ← newUser(usr)
          progress = prg.copy(user = user)
          userProgress ← repository.create(progress)
          currentUserProgress ← repository.getExerciseEvaluations(
            user = user,
            libraryName = prg.libraryName,
            sectionName = prg.sectionName)
        } yield currentUserProgress.size.equals(1) && currentUserProgress.head.equals(userProgress)

        assertConnectionIO(tx)
    }
  }

  ignore("user progress can be fetched by exercise") {
    forAll(maxDiscardedFactor(10000d)) {
      (usr: UserCreation.Request, prg: SaveUserProgress.Request) ⇒
        val tx: ConnectionIO[Boolean] = for {
          user ← newUser(usr)
          progress = prg.copy(user = user)
          userProgress ← repository.create(progress)
          currentUserProgress ← repository.getExerciseEvaluation(
            user = user,
            libraryName = prg.libraryName,
            sectionName = prg.sectionName,
            method = prg.method,
            version = prg.version
          )
        } yield currentUserProgress.forall(up ⇒ up.equals(userProgress))

        assertConnectionIO(tx)
    }
  }

  ignore("users progress can be queried by their ID") {
    forAll(maxDiscardedFactor(10000d)) {
      (usr: UserCreation.Request, prg: SaveUserProgress.Request) ⇒
        val tx: ConnectionIO[Boolean] = for {
          user ← newUser(usr)
          progress = prg.copy(user = user)
          userProgress      ← repository.create(progress)
          maybeUserProgress ← repository.findById(userProgress.id)
        } yield maybeUserProgress.forall(up ⇒ up.equals(userProgress))

        assertConnectionIO(tx)
    }

  }

  ignore("user progress can be deleted") {
    forAll(maxDiscardedFactor(10000d)) {
      (usr: UserCreation.Request, prg: SaveUserProgress.Request) ⇒
        val tx: ConnectionIO[Boolean] = for {
          user          ← newUser(usr)
          userProgress  ← repository.create(prg.copy(user = user))
          _             ← repository.delete(userProgress.id)
          maybeProgress ← repository.findById(userProgress.id)
        } yield maybeProgress.isEmpty

        assertConnectionIO(tx)
    }
  }

  ignore("all user progress records can be deleted") {
    forAll(maxDiscardedFactor(10000d)) {
      (usr: UserCreation.Request, prg: SaveUserProgress.Request) ⇒
        val tx: ConnectionIO[Boolean] = for {
          user          ← newUser(usr)
          userProgress  ← repository.create(prg.copy(user = user))
          _             ← repository.deleteAll()
          maybeProgress ← repository.findById(userProgress.id)
        } yield maybeProgress.isEmpty

        assertConnectionIO(tx)
    }
  }
}
