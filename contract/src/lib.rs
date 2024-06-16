#![no_std]


use soroban_sdk::{assert_with_error, contract, contracterror, contractimpl, contracttype, token::TokenClient, Address, Env};

#[contracttype]
#[derive(Clone, Debug)]
pub struct MandateConfig{
    pub amount: i128,
    pub start : u64,
    pub last_charged: u64,
    pub charge_interval: u64,
    pub shopper: Address,
    pub merchant: Address,
    pub token_id: Address,
}

#[derive(Clone)]
#[contracttype]
pub enum DataKey{
    MandateId,
    Mandate(u64),
}

#[contracterror]
#[derive(Copy, Clone, Eq, PartialEq, PartialOrd, Ord)]
#[repr(u32)]
pub enum Error {
    X = 1
}

pub fn get_and_inc_mandate_id(env: &Env) -> u64 {
    let id =  env
    .storage()
    .persistent()
    .get(&DataKey::MandateId)
    .unwrap_or( 0);

    env.storage().persistent().set(&DataKey::MandateId, &(id + 1));

    id
}

pub fn store_config(env: &Env, config: &MandateConfig) -> u64{
    let id = get_and_inc_mandate_id(env);
    env
    .storage()
    .persistent()
    .set(&DataKey::Mandate(id), config);

    id
}

pub fn get_config(env: &Env, mandate_id: u64) -> MandateConfig{
    env.storage()
    .persistent()
    .get(&DataKey::Mandate(mandate_id))
    .unwrap()
}


pub trait MandateContractTrait {
    fn create_mandate(env: Env, config: MandateConfig, untill: u32) -> u64;
    fn charge_mandate(env: Env, mandate_id: u64);
    fn revoke_mandate(env: Env, mandate_id: u64);
}

#[contract]
pub struct MandateContract;

#[contractimpl]
impl MandateContractTrait for MandateContract{
    fn create_mandate(env: Env, mut config: MandateConfig, untill: u32) -> u64{
        config.shopper.require_auth();

            //&(env.ledger().max_live_until_ledger() - 100)
        TokenClient::new(&env, &config.token_id)
        .approve(&config.shopper, &env.current_contract_address(), &(i128::MAX), &(untill));

        config.last_charged = 0;

        store_config(&env, &config)
    }

    fn charge_mandate(env: Env, mandate_id: u64){
        let mut mandate = get_config(&env, mandate_id);

        mandate.merchant.require_auth();
        assert_with_error!(&env, mandate.last_charged + mandate.charge_interval < env.ledger().timestamp(), Error::X);

        TokenClient::new(&env, &mandate.token_id)
        .transfer_from(&env.current_contract_address(), &mandate.shopper, &mandate.merchant, &mandate.amount);

        mandate.last_charged = env.ledger().timestamp();

        store_config(&env, &mandate);

    }

    fn revoke_mandate(env: Env, mandate_id: u64){
        let mandate = get_config(&env, mandate_id);

        mandate.shopper.require_auth();
        
        TokenClient::new(&env, &mandate.token_id)
        .approve(&mandate.shopper, &env.current_contract_address(), &0, &0);

        env.storage()
        .persistent()
        .remove(&DataKey::Mandate(mandate_id))
    }
}
