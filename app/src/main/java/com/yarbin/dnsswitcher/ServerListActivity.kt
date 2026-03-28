package com.yarbin.dnsswitcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections

class ServerListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ServerAdapter
    private val servers = mutableListOf<String>()
    private val serverStatus = mutableMapOf<String, CheckResult>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server_list)

        servers.addAll(loadServers())

        recyclerView = findViewById(R.id.recycler_servers)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ServerAdapter(
            servers = servers,
            statusMap = serverStatus,
            activeServerProvider = { getActiveServer() },
            onServerClick = { position -> onServerSelected(position) },
            onDeleteClick = { position -> onServerDeleteRequested(position) }
        )
        recyclerView.adapter = adapter

        val touchCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                source: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = source.adapterPosition
                val to = target.adapterPosition
                Collections.swap(servers, from, to)
                adapter.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val server = servers[position]
                servers.removeAt(position)
                serverStatus.remove(server)
                saveServers()
                adapter.notifyItemRemoved(position)
                Toast.makeText(
                    this@ServerListActivity,
                    getString(R.string.deleted_toast, server),
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun isLongPressDragEnabled() = true
            override fun isItemViewSwipeEnabled() = true

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                saveServers()
            }

            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float,
                actionState: Int, isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                    val itemView = viewHolder.itemView
                    val paint = Paint().apply { color = Color.parseColor("#FFF44336") }
                    c.drawRoundRect(
                        itemView.right + dX, itemView.top.toFloat(),
                        itemView.right.toFloat(), itemView.bottom.toFloat(),
                        12f * resources.displayMetrics.density,
                        12f * resources.displayMetrics.density,
                        paint
                    )
                    val textPaint = Paint().apply {
                        color = Color.WHITE
                        textSize = 14f * resources.displayMetrics.scaledDensity
                        isAntiAlias = true
                    }
                    val text = getString(R.string.swipe_delete)
                    val textWidth = textPaint.measureText(text)
                    if (-dX > textWidth + 32 * resources.displayMetrics.density) {
                        c.drawText(
                            text,
                            itemView.right - textWidth - 24 * resources.displayMetrics.density,
                            itemView.top + (itemView.height + textPaint.textSize) / 2f - 4f,
                            textPaint
                        )
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(touchCallback).attachToRecyclerView(recyclerView)

        findViewById<FloatingActionButton>(R.id.fab_add).setOnClickListener {
            showAddServerDialog()
        }

        checkAllServers()
    }

    private fun getActiveServer(): String? {
        return getSharedPreferences("dns_prefs", Context.MODE_PRIVATE)
            .getString("active_server", null)
    }

    private fun onServerSelected(position: Int) {
        val server = servers[position]
        getSharedPreferences("dns_prefs", Context.MODE_PRIVATE)
            .edit().putString("active_server", server).apply()
        Toast.makeText(this, getString(R.string.active_toast, server), Toast.LENGTH_SHORT).show()
        adapter.notifyDataSetChanged()
    }

    private fun onServerDeleteRequested(position: Int) {
        val server = servers[position]
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_title)
            .setMessage(server)
            .setPositiveButton(R.string.delete_button) { _, _ ->
                servers.removeAt(position)
                serverStatus.remove(server)
                saveServers()
                adapter.notifyItemRemoved(position)
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }

    private fun showAddServerDialog() {
        val container = FrameLayout(this)
        val input = EditText(this).apply {
            hint = getString(R.string.add_hint)
        }
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(48, 16, 48, 0) }
        container.addView(input, params)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_title)
            .setView(container)
            .setPositiveButton(R.string.add_button) { _, _ ->
                val value = input.text.toString().trim()
                if (value.isNotEmpty()) {
                    servers.add(value)
                    saveServers()
                    adapter.notifyItemInserted(servers.size - 1)
                    checkServer(value)
                }
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }

    private fun checkAllServers() {
        for (server in servers.toList()) {
            checkServer(server)
        }
    }

    private fun checkServer(server: String) {
        serverStatus[server] = CheckResult.Checking
        val index = servers.indexOf(server)
        if (index >= 0) adapter.notifyItemChanged(index)

        Thread {
            val result = try {
                val start = System.currentTimeMillis()
                val url = URL("https://$server/dns-query?dns=AAABAAABAAAAAAAAB2V4YW1wbGUDY29tAAABAAE")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/dns-message")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.responseCode
                val elapsed = System.currentTimeMillis() - start
                CheckResult.Ok(elapsed)
            } catch (_: Exception) {
                CheckResult.Fail
            }
            runOnUiThread {
                serverStatus[server] = result
                val i = servers.indexOf(server)
                if (i >= 0) adapter.notifyItemChanged(i)
            }
        }.start()
    }

    private fun loadServers(): List<String> {
        val prefs = getSharedPreferences("dns_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getString("servers", "") ?: ""
        return if (saved.isEmpty()) mutableListOf("dns.cloudflare.com", "dns.google")
        else saved.split(",").toMutableList()
    }

    private fun saveServers() {
        val prefs = getSharedPreferences("dns_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("servers", servers.joinToString(",")).apply()
    }

    sealed class CheckResult {
        object Checking : CheckResult()
        data class Ok(val latencyMs: Long) : CheckResult()
        object Fail : CheckResult()
    }

    private class ServerAdapter(
        private val servers: List<String>,
        private val statusMap: Map<String, CheckResult>,
        private val activeServerProvider: () -> String?,
        private val onServerClick: (Int) -> Unit,
        private val onDeleteClick: (Int) -> Unit
    ) : RecyclerView.Adapter<ServerAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val card: MaterialCardView = view.findViewById(R.id.card)
            val radioActive: RadioButton = view.findViewById(R.id.radio_active)
            val textName: TextView = view.findViewById(R.id.text_server_name)
            val textActiveLabel: TextView = view.findViewById(R.id.text_active_label)
            val textStatus: TextView = view.findViewById(R.id.text_status)
            val btnDelete: ImageButton = view.findViewById(R.id.btn_delete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_server_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val server = servers[position]
            val isActive = server == activeServerProvider()
            val ctx = holder.card.context

            holder.textName.text = server
            holder.radioActive.isChecked = isActive
            holder.textActiveLabel.visibility = if (isActive) View.VISIBLE else View.GONE

            val strokeWidthPx = if (isActive) {
                (2 * ctx.resources.displayMetrics.density).toInt()
            } else 0
            holder.card.strokeWidth = strokeWidthPx
            if (isActive) {
                holder.card.strokeColor = ctx.getColor(R.color.md_primary)
            }

            val status = statusMap[server]
            when (status) {
                is CheckResult.Checking -> {
                    holder.textStatus.visibility = View.VISIBLE
                    holder.textStatus.text = "…"
                    holder.textStatus.setTextColor(Color.GRAY)
                }
                is CheckResult.Ok -> {
                    holder.textStatus.visibility = View.VISIBLE
                    holder.textStatus.text = ctx.getString(R.string.status_ms, status.latencyMs)
                    holder.textStatus.setTextColor(Color.parseColor("#FF4CAF50"))
                }
                is CheckResult.Fail -> {
                    holder.textStatus.visibility = View.VISIBLE
                    holder.textStatus.text = ctx.getString(R.string.status_unreachable)
                    holder.textStatus.setTextColor(Color.parseColor("#FFF44336"))
                }
                null -> {
                    holder.textStatus.visibility = View.GONE
                }
            }

            holder.card.setOnClickListener { onServerClick(position) }
            holder.btnDelete.setOnClickListener { onDeleteClick(position) }
        }

        override fun getItemCount() = servers.size
    }
}
