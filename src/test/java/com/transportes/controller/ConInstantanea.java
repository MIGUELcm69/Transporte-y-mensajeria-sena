package com.transportes.controller;

/** DAO en memoria cuyo estado puede guardarse y restaurarse para simular un rollback. */
interface ConInstantanea {

    /** Copia el estado actual y devuelve la acción que lo restaura. */
    Runnable instantanea();
}
