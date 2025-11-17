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
    var isPurchased: Boolean = false
)

class ProfileCharacterFragment : Fragment() {

    private var _binding: FragmentProfileCharacterBinding? = null
    private val binding get() = _binding!!
    private lateinit var shopAdapter: AccessoryShopAdapter

    // Sample shop accessories - replace R.drawable.ic_* with your actual PNG drawable resources
    // For example: R.drawable.hat_blue, R.drawable.outfit_casual, etc.
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
        // Get the actual coin count from your CharacterManager
        val coins = CharacterManager.getCoins(requireContext())

        // Set the text of your TextView
        binding.coinsCountText.text = "$coins Coins"
    }

    private fun loadPurchasedAccessories() {
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", android.content.Context.MODE_PRIVATE)
        shopAccessories.forEach { accessory ->
            accessory.isPurchased = prefs.getBoolean("purchased_${accessory.id}", false)
        }
    }

    private fun setupShop() {
        shopAdapter = AccessoryShopAdapter(
            accessories = shopAccessories,
            onAccessoryClick = { accessory ->
                if (accessory.isPurchased) {
                    Toast.makeText(
                        requireContext(),
                        "You already own this item!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    showPurchaseDialog(accessory)
                }
            }
        )

        binding.shopRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.shopRecyclerView.adapter = shopAdapter

        // Show shop section
        binding.shopSection.visibility = View.VISIBLE
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
            // Mark as purchased
            accessory.isPurchased = true

            // Save purchase to SharedPreferences
            val prefs = requireContext().getSharedPreferences("TallyUpPrefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("purchased_${accessory.id}", true).apply()

            // Update UI
            updateCoinCount()
            shopAdapter.notifyDataSetChanged()

            Toast.makeText(
                requireContext(),
                "Successfully purchased ${accessory.name}!",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                requireContext(),
                "Purchase failed!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh the coin count in case the user earns/spends
        // coins and switches back to this tab.
        if (_binding != null) {
            updateCoinCount()
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

        if (accessory.isPurchased) {
            holder.purchasedBadge.visibility = View.VISIBLE
            holder.itemView.alpha = 0.6f
        } else {
            holder.purchasedBadge.visibility = View.GONE
            holder.itemView.alpha = 1.0f
        }

        holder.itemView.setOnClickListener {
            onAccessoryClick(accessory)
        }
    }

    override fun getItemCount() = accessories.size
}