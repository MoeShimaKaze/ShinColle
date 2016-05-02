package com.lulan.shincolle.entity.destroyer;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.entity.ExtendShipProps;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.ParticleHelper;


public class EntityDestroyerIkazuchi extends BasicEntityShipSmall {

	public boolean isGattai = false;
	
	
	public EntityDestroyerIkazuchi(World world) {
		super(world);
		this.setSize(0.6F, 1.5F);
		this.setStateMinor(ID.M.ShipType, ID.ShipType.DESTROYER);
		this.setStateMinor(ID.M.ShipClass, ID.Ship.DestroyerIkazuchi);
		this.setStateMinor(ID.M.DamageType, ID.ShipDmgType.DESTROYER);
		this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[ID.ShipConsume.DD]);
		this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[ID.ShipConsume.DD]);
		this.ModelPos = new float[] {0F, 13F, 0F, 50F};
		this.ExtProps = (ExtendShipProps) getExtendedProperties(ExtendShipProps.SHIP_EXTPROP_NAME);	
		
		//higher step
		this.stepHeight = 2F;
		
		this.initTypeModify();
		
		//set attack type
		this.StateFlag[ID.F.HaveRingEffect] = true;
		this.StateFlag[ID.F.AtkType_AirLight] = false;
		this.StateFlag[ID.F.AtkType_AirHeavy] = false;
		
		//gattai
		this.isGattai = false;
		
	}
	
	//for morph
	@Override
	public float getEyeHeight() {
		return 1.4F;
	}
	
	//equip type: 1:cannon+misc 2:cannon+airplane+misc 3:airplane+misc
	@Override
	public int getEquipType() {
		return 1;
	}
	
	@Override
	public void setAIList() {
		super.setAIList();
		//use range attack (light)
		this.tasks.addTask(11, new EntityAIShipRangeAttack(this));
	}
    
    //check entity state every tick
  	@Override
  	public void onLivingUpdate()
  	{
  		super.onLivingUpdate();
  		
  		//server side
  		if (!worldObj.isRemote)
  		{
  			if (this.ticksExisted % 128 == 0)
  			{
  				//add aura to master every 128 ticks
  				EntityPlayerMP player = (EntityPlayerMP) EntityHelper.getEntityPlayerByUID(this.getPlayerUID());
  				
  				if (getStateFlag(ID.F.IsMarried) && getStateFlag(ID.F.UseRingEffect) &&
  					getStateMinor(ID.M.NumGrudge) > 0 && player != null &&
  					getDistanceSqToEntity(player) < 256D)
  				{
  					//potion effect: id, time, level
  	  	  			player.addPotionEffect(new PotionEffect(Potion.damageBoost.id, 300, getStateMinor(ID.M.ShipLevel) / 50));
  				}
  				
  				//try gattai
  				tryGattai(this);
  			}
  		}
  		//client side
  		else
  		{
  			if (this.ticksExisted % 4 == 0)
  			{
  				if (getStateEmotion(ID.S.State) >= ID.State.EQUIP01 && !isSitting() && !getStateFlag(ID.F.NoFuel))
  				{
  					double smokeY = posY + 1.4D;
  					float addz = 0F;
  					
  					if (this.ridingEntity != null) addz = -0.2F;
  					
  					//計算煙霧位置
  	  				float[] partPos = ParticleHelper.rotateXZByAxis(-0.42F + addz, 0F, (this.renderYawOffset % 360) / 57.2957F, 1F);
  	  				//生成裝備冒煙特效
  	  				ParticleHelper.spawnAttackParticleAt(posX+partPos[1], smokeY, posZ+partPos[0], 0D, 0D, 0D, (byte)20);
  				}	
  			}
  		}
  		
  		//sync rotate when gattai
		if (this.ridingEntity instanceof EntityDestroyerInazuma)
		{
			this.renderYawOffset = ((EntityDestroyerInazuma) this.ridingEntity).renderYawOffset;
			this.prevRenderYawOffset = ((EntityDestroyerInazuma) this.ridingEntity).prevRenderYawOffset;
			this.rotationYaw = this.ridingEntity.rotationYaw;
			this.prevRotationYaw = this.ridingEntity.prevRotationYaw;
		}
  	}
  	
  	@Override
  	public boolean interact(EntityPlayer player) {	
		ItemStack itemstack = player.inventory.getCurrentItem();  //get item in hand
		
		//use cake to change state
		if(itemstack != null) {
			if(itemstack.getItem() == Items.cake) {
				this.setShipOutfit(player.isSneaking());
				return true;
			}
		}
		
		super.interact(player);
		return false;
  	}
  	
  	@Override
	public int getKaitaiType() {
		return 2;
	}
  	
  	@Override
	public double getMountedYOffset() {
  		if(this.isSitting()) {
  			return (double)this.height * 0.15F;
  		}
  		else {
  			return (double)this.height * 0.47F;
  		}
	}

	@Override
	public void setShipOutfit(boolean isSneaking) {
		switch(getStateEmotion(ID.S.State)) {
		case ID.State.NORMAL:
			setStateEmotion(ID.S.State, ID.State.EQUIP00, true);
			break;
		case ID.State.EQUIP00:
			setStateEmotion(ID.S.State, ID.State.EQUIP01, true);
			break;
		case ID.State.EQUIP01:
			setStateEmotion(ID.S.State, ID.State.EQUIP02, true);
			break;
		default:
			setStateEmotion(ID.S.State, ID.State.NORMAL, true);
			break;
		}
	}
	
	@Override
    public boolean attackEntityFrom(DamageSource attacker, float atk) {
		boolean dd = super.attackEntityFrom(attacker, atk);
		
		if (dd)
		{
			//cancel gattai
			if (this.ridingEntity instanceof EntityDestroyerInazuma)
			{
				this.isGattai = false;
				this.mountEntity(null);
			}
		}
		
		return dd;
	}
	
	//檢查是否可以合體
	public static void tryGattai(BasicEntityShip ship)
	{
		//not sitting, hp > 50%, 
		if (ship != null && !ship.getStateFlag(ID.F.NoFuel) && !ship.isSitting() && ship.getHealth() > ship.getMaxHealth() * 0.5F)
		{
			//check ship is rai or den
			boolean isRai = (ship.getShipClass() == ID.Ship.DestroyerIkazuchi);
			boolean isDen = (ship.getShipClass() == ID.Ship.DestroyerInazuma);
			
			if(!isRai && !isDen) return;
			
			//get nearby ship
            List<BasicEntityShip> slist = null;
            slist = ship.worldObj.getEntitiesWithinAABB(BasicEntityShip.class, ship.boundingBox.expand(1.5D, 1D, 1.5D));

            if (slist != null && !slist.isEmpty())
            {
            	for (BasicEntityShip s : slist)
            	{
            		if (s != null && ((isRai && s.getShipClass() == ID.Ship.DestroyerInazuma) || (isDen && s.getShipClass() == ID.Ship.DestroyerIkazuchi)) &&
            			EntityHelper.checkSameOwner(ship, s) && s.isEntityAlive() && !s.isRiding() &&
            			s.riddenByEntity == null)
            		{
            			if (isRai)
            			{
            				applyGattai(ship, s);
            				return;
            			}
            			else if (isDen)
            			{
            				applyGattai(s, ship);
            				return;
            			}
            		}
            	}
            }//end get ship
		}//end can gattai
	}
	
	//雷電合體方法
	private static void applyGattai(BasicEntityShip rai, BasicEntityShip den)
	{
		//not null, not riding, same owner, not sitting
		if (rai != null && den != null &&
			rai instanceof EntityDestroyerIkazuchi &&
			den instanceof EntityDestroyerInazuma)
		{
			((EntityDestroyerIkazuchi)rai).isGattai = true;
			((EntityDestroyerInazuma)den).isGattai = true;
			rai.mountEntity(den);
		}
	}
  	

}