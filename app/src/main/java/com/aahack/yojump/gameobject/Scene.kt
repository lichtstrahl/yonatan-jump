package com.aahack.yojump.gameobject

import android.graphics.Canvas
import android.graphics.RectF
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created on 15.12.2018.
 * @author dhabensky <dhabensky@yandex.ru>
 */
class Scene {

	private val objects = LinkedList<GameObject>()
	private lateinit var camera: Camera
	private lateinit var player: Player
	private lateinit var score: ScoreLabel

	var lastFrameMillis: Long = 0L

	private val playerBounds = RectF()
	private val playerBoundsCopy = RectF()
	private val bounds = RectF()

	public var gameOver = false

	private val objToRemove = arrayListOf<GameObject>()
	private val objToAdd = arrayListOf<GameObject>()

	fun render(canvas: Canvas) {

		val curMillis = System.currentTimeMillis()
		if (lastFrameMillis == 0L) {
			lastFrameMillis = curMillis
		}
		val delta = (curMillis - lastFrameMillis) / 1000f
		lastFrameMillis = curMillis


		for (obj in objects) {
			obj.update(delta)
		}

		processCollisions(delta)

		canvas.matrix = camera.getMatrix()

		for (obj in objects) {
			obj.render(canvas)
		}

		for (obj in objToRemove) {
			Log.d("GENERATION", "removing ${obj}")
			objects.remove(obj)
		}
		objToRemove.clear()

		for (obj in objToAdd) {
			objects.add(obj)
		}
		objToAdd.clear()

		score.render(canvas)

		val iter = objects.iterator()
		while (iter.hasNext()) {
			val obj = iter.next()
			if (obj.tag == "death") continue
			if (obj.pos.x < camera.pos.x - 3000f) {
				iter.remove()
				Log.d("GENERATION", "pop object")
			}
		}
	}

	private fun processCollisions(delta: Float) {

		player.getBounds(playerBounds)
		if (player.velocity.y > 0) {
			bounds.top -= player.velocity.y * delta
		}

		var colliding = false

		for (obj in objects) {
			playerBoundsCopy.set(playerBounds)
			obj.getBounds(bounds)

			if (playerBoundsCopy.intersect(bounds)) {
				if (obj.tag == "block") {
					log("collision with block")
					processBlockCollision(obj, playerBoundsCopy)
					colliding = true
				}
				else if (obj.tag == "collectable") {
					processCollectableCollision(obj)
				}
				else if (obj.tag == "death") {
					processDeathCollision(obj)
				}
			}
		}

		player.isColliding = colliding
	}

	private fun processBlockCollision(obj: GameObject, collision: RectF) {

		if (player.velocity.y < 0) {
			log("negative velocity. END")
			return
		}

		log("collision: $collision, player: $playerBounds")

		if (collision.bottom != playerBounds.bottom) {
			log("bottom not match. END")
			return
		}

		val threshold = 16f
		val nearTop = collision.height() < threshold

		val newCollision = nearTop || !player.isColliding

		if (newCollision) {
			log("new collision. END")
			player.pos.y = bounds.top - player.h
			player.velocity.y = 0f
			player.resetJumpCount()

			if (obj is Block && obj.destructible) {
				obj.scheduleDestruct()
			}
		}
		else {
			log("ignoring collision. END")
		}
	}

	private fun processCollectableCollision(obj: GameObject) {
		player.collect()
		objToRemove.add(obj)
	}

	private fun processDeathCollision(obj: GameObject) {
		gameOver = true
	}

	fun addObject(gameObject: GameObject) {
		objects.add(gameObject)
	}

	fun setCamera(camera: Camera) {
		addObject(camera)
		this.camera = camera
	}

	fun setPlayer(player: Player) {
		addObject(player)
		this.player = player
	}

	fun setScore(score: ScoreLabel) {
		addObject(score)
		this.score = score
	}

	companion object {
		const val TAG = "COLLISION"
		private val formatter = SimpleDateFormat("mm:ss:SSS")

		fun log(message: String) {
			Log.d(TAG, formatter.format(Date()) + " " + message)
		}
	}

	fun getScore(): String? {
		return score.getScore()
	}

	fun delayedAddObject(obj: GameObject) {
		objToAdd.add(obj)
	}

	fun delayedRemoveObject(obj: GameObject) {
		objToRemove.add(obj)
	}

}
