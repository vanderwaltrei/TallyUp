package za.ac.iie.TallyUp.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.databinding.FragmentProfileCharacterBinding
import za.ac.iie.TallyUp.utils.CharacterManager

data class ShopAccessory(
    val id: String,
    val name: String,
    val imageRes: Int,
    val price: Int,
    var isPurchased: Boolean = false,
    var isEquipped: Boolean = false
)

class ProfileCharacterFragment : Fragment() {

    private var _binding: FragmentProfileCharacterBinding? = null
    private val binding get() = _binding!!
    private lateinit var shopAdapter: AccessoryShopAdapter

    private val shopAccessories = mutableListOf(
        ShopAccessory("luna_gamer", "Gamer Luna", R.drawable.luna_gamer, 50, false),
        ShopAccessory("luna_goddess", "Light Luna", R.drawable.luna_goddess, 50, false),
        ShopAccessory("luna_gothic", "Gothic Luna", R.drawable.luna_gothic, 50, false),
        ShopAccessory("luna_strawberry", "Strawberry Luna", R.drawable.luna_strawberry, 50, false),
        ShopAccessory("max_light", "Light Max", R.drawable.max_light, 50, false),
        ShopAccessory("max_villain", "Villain Max", R.drawable.max_villain, 50, false)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileCharacterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateCoinCount()
        loadPurchasedAccessories()
        setupShop()
    }

    @SuppressLint("SetTextI18n")
    private fun updateCoinCount() {
        val coins = CharacterManager.getCoins(requireContext())
        binding.coinsCountText.text = "$coins Coins"
    }

    private fun loadPurchasedAccessories() {
        val equippedCharacter = CharacterManager.getEquippedCharacter(requireContext())

        shopAccessories.forEach { accessory ->
            accessory.isPurchased = CharacterManager.isPurchased(requireContext(), accessory.id)
            accessory.isEquipped = (accessory.id == equippedCharacter)
        }
    }

    private fun setupShop() {
        shopAdapter = AccessoryShopAdapter(
            accessories = shopAccessories,
            onAccessoryClick = { accessory ->
                if (accessory.isPurchased) {
                    showEquipDialog(accessory)
                } else {
                    showPurchaseDialog(accessory)
                }
            }
        )

        binding.shopRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.shopRecyclerView.adapter = shopAdapter

        binding.shopSection.visibility = View.VISIBLE
    }

    private fun showEquipDialog(accessory: ShopAccessory) {
        if (accessory.isEquipped) {
            // Show option to unequip
            AlertDialog.Builder(requireContext())
                .setTitle("Unequip ${accessory.name}?")
                .setMessage("Do you want to switch back to your original character?")
                .setPositiveButton("Unequip") { _, _ ->
                    unequipAccessory(accessory)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            // Show option to equip
            AlertDialog.Builder(requireContext())
                .setTitle("Equip ${accessory.name}?")
                .setMessage("This will become your active character throughout the app.")
                .setPositiveButton("Equip") { _, _ ->
                    equipAccessory(accessory)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun equipAccessory(accessory: ShopAccessory) {
        // Unequip any currently equipped accessory
        shopAccessories.forEach { it.isEquipped = false }

        // Equip the selected accessory
        accessory.isEquipped = true
        CharacterManager.saveEquippedCharacter(requireContext(), accessory.id)

        shopAdapter.notifyDataSetChanged()

        Toast.makeText(
            requireContext(),
            "${accessory.name} equipped! Your character has been updated.",
            Toast.LENGTH_SHORT
        ).show()

        // Refresh the UI to show the new character
        refreshParentFragment()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun unequipAccessory(accessory: ShopAccessory) {
        accessory.isEquipped = false
        CharacterManager.saveEquippedCharacter(requireContext(), null)

        shopAdapter.notifyDataSetChanged()

        Toast.makeText(
            requireContext(),
            "Switched back to original character",
            Toast.LENGTH_SHORT
        ).show()

        refreshParentFragment()
    }

    private fun showPurchaseDialog(accessory: ShopAccessory) {
        val currentCoins = CharacterManager.getCoins(requireContext())

        if (currentCoins < accessory.price) {
            Toast.makeText(
                requireContext(),
                "Not enough coins! You need ${accessory.price - currentCoins} more coins.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Purchase ${accessory.name}")
            .setMessage("Do you want to buy ${accessory.name} for ${accessory.price} coins?")
            .setPositiveButton("Buy") { _, _ ->
                purchaseAccessory(accessory)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun purchaseAccessory(accessory: ShopAccessory) {
        val success = CharacterManager.spendCoins(requireContext(), accessory.price)

        if (success) {
            accessory.isPurchased = true
            CharacterManager.setPurchased(requireContext(), accessory.id, true)

            updateCoinCount()
            shopAdapter.notifyDataSetChanged()

            // Ask if they want to equip it now
            AlertDialog.Builder(requireContext())
                .setTitle("Purchase Successful!")
                .setMessage("Would you like to equip ${accessory.name} now?")
                .setPositiveButton("Equip Now") { _, _ ->
                    equipAccessory(accessory)
                }
                .setNegativeButton("Later", null)
                .show()
        } else {
            Toast.makeText(
                requireContext(),
                "Purchase failed!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun refreshParentFragment() {
        // Notify parent fragment to refresh
        parentFragment?.let {
            if (it is ProfileFragment) {
                // ProfileFragment will automatically refresh when we resume
                activity?.recreate() // Optional: Force full activity refresh
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            updateCoinCount()
            loadPurchasedAccessories()
            shopAdapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Adapter for the shop items
class AccessoryShopAdapter(
    private val accessories: List<ShopAccessory>,
    private val onAccessoryClick: (ShopAccessory) -> Unit
) : RecyclerView.Adapter<AccessoryShopAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: android.widget.ImageView = itemView.findViewById(R.id.accessoryImage)
        val nameText: android.widget.TextView = itemView.findViewById(R.id.accessoryName)
        val priceText: android.widget.TextView = itemView.findViewById(R.id.accessoryPrice)
        val purchasedBadge: android.widget.TextView = itemView.findViewById(R.id.purchasedBadge)
        val equippedBadge: android.widget.TextView = itemView.findViewById(R.id.equippedBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shop_accessory, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val accessory = accessories[position]

        holder.imageView.setImageResource(accessory.imageRes)
        holder.nameText.text = accessory.name
        holder.priceText.text = "${accessory.price} coins"

        if (accessory.isEquipped) {
            holder.equippedBadge.visibility = View.VISIBLE
            holder.purchasedBadge.visibility = View.GONE
            holder.itemView.alpha = 1.0f
        } else if (accessory.isPurchased) {
            holder.purchasedBadge.visibility = View.VISIBLE
            holder.equippedBadge.visibility = View.GONE
            holder.itemView.alpha = 0.8f
        } else {
            holder.purchasedBadge.visibility = View.GONE
            holder.equippedBadge.visibility = View.GONE
            holder.itemView.alpha = 1.0f
        }

        holder.itemView.setOnClickListener {
            onAccessoryClick(accessory)
        }
    }

    override fun getItemCount() = accessories.size
}