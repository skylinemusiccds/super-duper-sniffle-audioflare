package com.universe.audioflare.adapter.account

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.universe.audioflare.data.db.entities.GoogleAccountEntity
import com.universe.audioflare.databinding.ItemAccountBinding

class AccountAdapter(private val accountList: ArrayList<GoogleAccountEntity>) :
    RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

    interface OnAccountClickListener {
        fun onAccountClick(pos: Int)
    }

    fun setOnAccountClickListener(listener: OnAccountClickListener) {
        this.listener = listener
    }

    lateinit var listener: OnAccountClickListener

    fun getAccountList(): List<GoogleAccountEntity> {
        return accountList
    }


    inner class AccountViewHolder(
        val binding: ItemAccountBinding,
        private val listener: OnAccountClickListener
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onAccountClick(bindingAdapterPosition)
            }
        }

        fun bind(account: GoogleAccountEntity) {
            with(binding) {
                tvAccountName.text = account.name
                tvEmail.text = account.email
                if (account.isUsed) {
                    tvIsUsed.visibility = ViewGroup.VISIBLE
                } else {
                    tvIsUsed.visibility = ViewGroup.GONE
                }
                ivAccount.load(account.thumbnailUrl) {
                    crossfade(true)
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_report_image)
                }
            }
        }
    }

    fun updateAccountList(newAccountList: List<GoogleAccountEntity>) {
        accountList.clear()
        accountList.addAll(newAccountList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val binding = ItemAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AccountViewHolder(binding, listener)
    }

    override fun getItemCount(): Int {
        return accountList.size
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        holder.bind(accountList[position])
    }
}